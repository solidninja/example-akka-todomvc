package is
package solidninja
package todomvc
package protocol

import java.util.UUID

final case class Todo(id: UUID, title: String, completed: Boolean) {
  def withoutId: NewTodo = NewTodo(title, completed)
}

final case class NewTodo(title: String, completed: Boolean) {
  def withId(id: UUID): Todo = Todo(id, title, completed)
  def withRandomUuid: Todo = withId(UUID.randomUUID())
}

sealed trait TodoCommand

object TodoCommand {
  final case class AddTodo(todo: NewTodo) extends TodoCommand
  final case class CompleteTodo(id: UUID) extends TodoCommand
  final case class UpdateTodo(todo: Todo) extends TodoCommand
}

sealed trait TodoQuery

object TodoQuery {
  final case object GetTodos extends TodoQuery
  final case class GetTodo(id: UUID) extends TodoQuery
}

sealed trait TodoEvent

object TodoEvent {
  final case class TodoUpdated(todo: Todo) extends TodoEvent
}

sealed trait TodoQueryResponse

object TodoQueryResponse {
  final case object NotFound extends TodoQueryResponse
  final case class Found(todo: Todo) extends TodoQueryResponse
  final case class FoundAll(todos: List[Todo]) extends TodoQueryResponse
}
