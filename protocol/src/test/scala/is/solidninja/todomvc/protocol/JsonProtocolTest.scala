package is
package solidninja
package todomvc
package protocol

import java.util.UUID

import io.circe.literal._
import io.circe.syntax._
import is.solidninja.todomvc.protocol.TodoCommand.{AddTodo, CompleteTodo, UpdateTodo}
import is.solidninja.todomvc.protocol.TodoEvent.TodoUpdated
import is.solidninja.todomvc.protocol.TodoQuery.{GetTodo, GetTodos}
import org.scalatest.{FreeSpec, Matchers}

class JsonProtocolTest extends FreeSpec with Matchers {
  import JsonProtocol._

  "Todo should be serialized to JSON and back again" in {
    val js = json"""{
      "id": "64e9b9dd-f99f-491b-9148-fc73256bb19e",
      "title": "Do laundry",
      "completed": false
    }"""

    val todo =
      Todo(id = UUID.fromString("64e9b9dd-f99f-491b-9148-fc73256bb19e"), title = "Do laundry", completed = false)

    todo.asJson should equal(js)
    js.as[Todo] should equal(Right(todo))
  }

  "NewTodo should be serialized to JSON and back again" in {
    val js =
      json"""{
        "title": "Paint the shed",
        "completed": false
      }"""

    val todo =
      NewTodo(title = "Paint the shed", completed = false)

    todo.asJson should equal(js)
    js.as[NewTodo] should equal(Right(todo))
  }

  "TodoCommand" - {
    val todo = Todo(
      id = UUID.fromString("ca16193a-b359-42e3-b5cc-bea805aca7ae"),
      title = "Get brexit done",
      completed = false
    )

    "AddTodo should be serialized to JSON and back again" in {
      val js = json"""{
        "AddTodo" : {
          "todo" : {
            "title" : "Get brexit done",
            "completed" : false
          }
        }
      }"""

      val addTodo: TodoCommand = AddTodo(todo.withoutId)

      js.as[TodoCommand] should equal(Right(addTodo))
      addTodo.asJson should equal(js)
    }

    "CompleteTodo should be serialized to JSON and back again" in {
      val js = json"""{
        "CompleteTodo" : {
          "id" : "ca16193a-b359-42e3-b5cc-bea805aca7ae"
        }
      }"""

      val completeTodo: TodoCommand = CompleteTodo(todo.id)

      js.as[TodoCommand] should equal(Right(completeTodo))
      completeTodo.asJson should equal(js)
    }

    "UpdateTodo should be serialized to JSON and back again" in {
      val js = json"""{
        "UpdateTodo" : {
          "todo" : {
            "id" : "ca16193a-b359-42e3-b5cc-bea805aca7ae",
            "title" : "Get brexit done",
            "completed" : true
          }
        }
      }"""

      val updateTodo: TodoCommand = UpdateTodo(todo.copy(completed = true))

      js.as[TodoCommand] should equal(Right(updateTodo))
      updateTodo.asJson should equal(js)
    }
  }

  "TodoEvent" - {
    val todo = Todo(
      id = UUID.fromString("9151f164-1392-44d9-8ece-1f75f6654365"),
      title = "Bake a cake",
      completed = true
    )

    "TodoUpdated should be serialized to JSON and back again" in {
      val js = json"""{
        "TodoUpdated" : {
          "todo" : {
            "id" : "9151f164-1392-44d9-8ece-1f75f6654365",
            "title" : "Bake a cake",
            "completed" : true
          }
        }
      }"""

      val todoUpdated: TodoEvent = TodoUpdated(todo)

      js.as[TodoEvent] should equal(Right(todoUpdated))
      todoUpdated.asJson should equal(js)
    }
  }

  "TodoQuery" - {
    "GetTodos should be serialized to JSON and back again" in {
      val js =
        json"""{
          "GetTodos": {
          }
        }"""

      val getTodos: TodoQuery = GetTodos

      js.as[TodoQuery] should equal(Right(getTodos))
      getTodos.asJson should equal(js)
    }

    "GetTodo should be serialized to JSON and back again" in {
      val js =
        json"""{
          "GetTodo": {
            "id": "1e31a42a-80ab-479b-8a1e-04b8b67c89bd"
          }
        }"""

      val getTodo: TodoQuery = GetTodo(UUID.fromString("1e31a42a-80ab-479b-8a1e-04b8b67c89bd"))

      js.as[TodoQuery] should equal(Right(getTodo))
      getTodo.asJson should equal(js)
    }
  }

  "TodoQueryResponse" - {
    "NotFound should be serialized to JSON and back again" in {}

    "Found should be serialized to JSON and back again" in {}

    "FoundAll should be serialized to JSON and back again" in {}
  }

}
