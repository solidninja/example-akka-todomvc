package is
package solidninja
package todomvc
package server

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging

object Main extends App with StrictLogging {
  locally {
    ActorSystem[Done](
      Behaviors.setup[Done] { ctx =>
        // setup typed actors
        val todoStoreRef = ctx.spawn(TodoStore(), "TodoStore")

        // untyped setup for http
        implicit val cas: ClassicActorSystem = ctx.system.toClassic
        implicit val ec: ExecutionContext = ctx.executionContext
        implicit val sh: Scheduler = cas.scheduler.toTyped
        implicit val timeout: Timeout = 5.seconds

        val routes = TodoService.routes(todoStoreRef)
        Http().newServerAt("0.0.0.0", 9090).bindFlow(routes).onComplete {
          case Success(bound) =>
            logger.info(s"Todo server up at ${bound.localAddress}")
          case Failure(e) =>
            logger.error("Failed to bind todo server", e)
            ctx.self ! Done
        }

        Behaviors.receiveMessage { case Done =>
          Behaviors.stopped
        }
      },
      "TodoServer"
    )
  }
}
