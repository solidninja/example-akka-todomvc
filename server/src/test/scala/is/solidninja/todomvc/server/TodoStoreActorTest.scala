package is.solidninja.todomvc.server

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import is.solidninja.todomvc.protocol.{NewTodo, TodoCommand, TodoEvent}
import is.solidninja.todomvc.server.TodoActorProtocol.{CommandReplyMessage, EventReply}
import org.scalatest.Inside
import org.scalatest.freespec.AnyFreeSpecLike

// FIXME - some bug with classloader so that it doesn't pick up the default application.conf
class TodoStoreActorTest
    extends ScalaTestWithActorTestKit("""
    include "application.conf"

    akka.actor.serialize-messages = on
  """.stripMargin)
    with AnyFreeSpecLike
    with Inside {

  val store: ActorRef[TodoStore.Command] = spawn(TodoStore())

  "TodoStore" - {
    "should be able to receive a new todo" in {
      val res = store ? (replyTo =>
        CommandReplyMessage(replyTo, TodoCommand.AddTodo(NewTodo("Wash dishes", completed = false)))
      )

      inside(res.futureValue) { case EventReply(u: TodoEvent.TodoUpdated) =>
        u.todo.title should equal("Wash dishes")
      }
    }
  }

}
