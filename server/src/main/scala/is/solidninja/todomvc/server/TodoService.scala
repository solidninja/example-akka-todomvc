package is
package solidninja
package todomvc
package server

import akka.NotUsed
import akka.actor.typed.{ActorRef, Scheduler}
import akka.stream.scaladsl.{BroadcastHub, Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import is.solidninja.todomvc.protocol._
import is.solidninja.todomvc.server.TodoActorProtocol.{
  CommandReplyMessage,
  EventReply,
  QueryReply,
  QueryReplyMessage,
  Reply
}
import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object TodoServiceEndpoints extends JsonProtocol {
  import sttp.tapir.generic.auto._

  private val base: Endpoint[Unit, String, Unit, Any] =
    endpoint.in("todo").errorOut(plainBody[String])

  val listTodos: Endpoint[Unit, String, Vector[Todo], Any] = base
    .in("list")
    .get
    .out(jsonBody[Vector[Todo]])
    .description("list all todos")

  val addTodo: Endpoint[NewTodo, String, Todo, Any] = base
    .in(
      jsonBody[NewTodo]
        .description("The todo to add to the list")
        .example(NewTodo("Wash socks", completed = false))
    )
    .post
    .out(jsonBody[Todo])
    .description("Add a new todo")

  val getTodo: Endpoint[UUID, String, Option[Todo], Any] = base
    .in(path[UUID].name("uuid"))
    .get
    .out(jsonBody[Option[Todo]])
    .description("Get a todo by its uuid")

  val completeTodo: Endpoint[UUID, String, Option[Todo], Any] = base
    .in("complete" / path[UUID].name("uuid"))
    .put
    .out(jsonBody[Option[Todo]])
    .description("Mark a todo as completed")

  val changelogStream: Endpoint[Unit, String, AkkaStreams.Pipe[String, Change[Todo]], AkkaStreams with WebSockets] =
    base
      .in("stream")
      .out(webSocketBody[String, CodecFormat.TextPlain, Change[Todo], CodecFormat.Json](AkkaStreams))
      .description("Stream out all the todos that are added")

  val All = List(listTodos, addTodo, getTodo, completeTodo, changelogStream)
}

object TodoService {
  import AkkaHttpServerInterpreter.toRoute
  import akka.actor.typed.scaladsl.AskPattern._
  import akka.http.scaladsl.server.Directives.concat
  import akka.http.scaladsl.server.Route

  def routes(
      store: ActorRef[TodoStore.Command]
  )(implicit ec: ExecutionContext, timeout: Timeout, scheduler: Scheduler, mat: Materializer): Route = {

    val (changeQ, changeSrcOrig) = Source.queue[Change[Todo]](16, OverflowStrategy.dropHead).preMaterialize()
    val changeSrc = changeSrcOrig.runWith(BroadcastHub.sink(16))
    locally {
      changeSrc.runWith(Sink.ignore)
    }

    def queryTodos(): Future[Either[String, Vector[Todo]]] =
      (store ? (replyTo => QueryReplyMessage(replyTo, TodoQuery.GetTodos))).flatMap(mapQueryReply {
        case TodoQueryResponse.FoundAll(todos) => Future.successful(Right(todos.toVector))
        case _                                 => Future.successful(unexpectedQueryReply)
      })

    val list = toRoute(TodoServiceEndpoints.listTodos)(_ => queryTodos())

    val add = toRoute(TodoServiceEndpoints.addTodo)(todo =>
      (store ? (replyTo => CommandReplyMessage(replyTo, TodoCommand.AddTodo(todo)))).flatMap(mapEventReply {
        case TodoEvent.TodoUpdated(todo) => changeQ.offer(Change.Add(todo)).map(_ => Right(todo))
        case _                           => Future.successful(unexpectedEventReply)
      })
    )

    val get = toRoute(TodoServiceEndpoints.getTodo)(id =>
      (store ? (replyTo => QueryReplyMessage(replyTo, TodoQuery.GetTodo(id))))
        .flatMap(mapQueryReply {
          case TodoQueryResponse.Found(todo) => Future.successful(Right(Some(todo)))
          case TodoQueryResponse.NotFound    => Future.successful(Right(None))
          case _                             => Future.successful(unexpectedQueryReply)
        })
    )

    val complete = toRoute(TodoServiceEndpoints.completeTodo)(id =>
      (store ? (replyTo => CommandReplyMessage(replyTo, TodoCommand.CompleteTodo(id))))
        .flatMap {
          case EventReply(TodoEvent.TodoUpdated(todo)) =>
            changeQ.offer(Change.Update(todo)).map(_ => Right(Some(todo)))
          case QueryReply(TodoQueryResponse.NotFound) => Future.successful(Right(None))
          case EventReply(_)                          => Future.successful(unexpectedEventReply)
          case QueryReply(_)                          => Future.successful(unexpectedQueryReply)
        }
    )

    val stream = toRoute(TodoServiceEndpoints.changelogStream) { _ =>
      val flow: Flow[String, Change[Todo], NotUsed] = Flow.fromSinkAndSource[String, Change[Todo]](
        Sink.ignore,
        Source.futureSource(queryTodos().flatMap {
          case Left(err)           => Future.failed(new RuntimeException(err))
          case Right(initialTodos) => Future.successful(Source(initialTodos.map(Change.Add(_))).concat(changeSrc))
        })
      )

      Future.successful(Right(flow))
    }

    // docgen
    val openApiDocs: OpenAPI = OpenAPIDocsInterpreter.toOpenAPI(TodoServiceEndpoints.All, "TodoMVC service", "0.1.0")
    val openApiYml: String = openApiDocs.toYaml

    concat(list, add, get, complete, stream, new SwaggerAkka(openApiYml).routes)
  }

  private def mapQueryReply[A](
      f: TodoQueryResponse => Future[Either[String, A]]
  ): Reply => Future[Either[String, A]] = {
    case QueryReply(response) => f(response)
    case EventReply(_)        => Future.successful(unexpectedEventReply)
  }

  private def mapEventReply[A](f: TodoEvent => Future[Either[String, A]]): Reply => Future[Either[String, A]] = {
    case EventReply(event) => f(event)
    case QueryReply(_)     => Future.successful(unexpectedQueryReply)
  }

  private def unexpectedEventReply[A]: Either[String, A] = Left("BUG: Unexpected event reply")
  private def unexpectedQueryReply[A]: Either[String, A] = Left("BUG: Unexpected query reply")

}
