package is
package solidninja
package todomvc
package server

import java.util.UUID

import scala.concurrent.ExecutionContext

import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import is.solidninja.todomvc.protocol.{
  JsonProtocol,
  NewTodo,
  Todo,
  TodoCommand,
  TodoEvent,
  TodoQuery,
  TodoQueryResponse
}
import is.solidninja.todomvc.server.TodoActorProtocol.{
  CommandReplyMessage,
  EventReply,
  QueryReply,
  QueryReplyMessage,
  Reply
}
import tapir._
import tapir.json.circe._
import tapir.server.akkahttp._
import tapir.docs.openapi._
import tapir.openapi.OpenAPI
import tapir.openapi.circe.yaml._
import tapir.swagger.akkahttp.SwaggerAkka

object TodoServiceEndpoints extends JsonProtocol {
  private val base: Endpoint[Unit, String, Unit, Nothing] =
    endpoint.in("todo").errorOut(plainBody[String])

  val listTodos = base
    .in("list")
    .get
    .out(jsonBody[Vector[Todo]])
    .description("list all todos")

  val addTodo = base
    .in(
      jsonBody[NewTodo]
        .description("The todo to add to the list")
        .example(NewTodo("Wash socks", completed = false))
    )
    .post
    .out(jsonBody[Todo])
    .description("Add a new todo")

  val getTodo = base
    .in(path[UUID].name("uuid"))
    .get
    .out(jsonBody[Option[Todo]])
    .description("Get a todo by its uuid")

  val completeTodo = base
    .in("complete" / path[UUID].name("uuid"))
    .put
    .out(jsonBody[Option[Todo]])
    .description("Mark a todo as completed")

  val All = List(listTodos, addTodo, getTodo, completeTodo)
}

object TodoService {
  import akka.actor.typed.scaladsl.AskPattern._
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.Route

  def routes(
      store: ActorRef[TodoStore.Command]
  )(implicit ec: ExecutionContext, timeout: Timeout, scheduler: Scheduler): Route = {

    val list =
      TodoServiceEndpoints.listTodos
        .toRoute(
          _ =>
            (store ? (replyTo => QueryReplyMessage(replyTo, TodoQuery.GetTodos))).map(mapQueryReply {
              case TodoQueryResponse.FoundAll(todos) => Right(todos.toVector)
              case _                                 => unexpectedQueryReply
            })
        )

    val add = TodoServiceEndpoints.addTodo
      .toRoute(
        todo =>
          (store ? (replyTo => CommandReplyMessage(replyTo, TodoCommand.AddTodo(todo)))).map(mapEventReply {
            case TodoEvent.TodoUpdated(todo) => Right(todo)
            case _                           => unexpectedEventReply
          })
      )

    val get = TodoServiceEndpoints.getTodo
      .toRoute(
        id =>
          (store ? (replyTo => QueryReplyMessage(replyTo, TodoQuery.GetTodo(id))))
            .map(mapQueryReply {
              case TodoQueryResponse.Found(todo) => Right(Some(todo))
              case TodoQueryResponse.NotFound    => Right(None)
              case _                             => unexpectedQueryReply
            })
      )

    val complete = TodoServiceEndpoints.completeTodo
      .toRoute(
        id =>
          (store ? (replyTo => CommandReplyMessage(replyTo, TodoCommand.CompleteTodo(id))))
            .map {
              case EventReply(TodoEvent.TodoUpdated(todo)) => Right(Some(todo))
              case QueryReply(TodoQueryResponse.NotFound)  => Right(None)
              case EventReply(_)                           => unexpectedEventReply
              case QueryReply(_)                           => unexpectedQueryReply
            }
      )

    // docgen
    val openApiDocs: OpenAPI = TodoServiceEndpoints.All.toOpenAPI("TodoMVC service", "0.1.0")
    val openApiYml: String = openApiDocs.toYaml

    concat(list, add, get, complete, new SwaggerAkka(openApiYml).routes)
  }

  private def mapQueryReply[A](f: TodoQueryResponse => Either[String, A]): Reply => Either[String, A] = {
    case QueryReply(response) => f(response)
    case EventReply(_)        => unexpectedEventReply
  }

  private def mapEventReply[A](f: TodoEvent => Either[String, A]): Reply => Either[String, A] = {
    case EventReply(event) => f(event)
    case QueryReply(_)     => unexpectedQueryReply
  }

  private def unexpectedEventReply[A]: Either[String, A] = Left("BUG: Unexpected event reply")
  private def unexpectedQueryReply[A]: Either[String, A] = Left("BUG: Unexpected query reply")

}
