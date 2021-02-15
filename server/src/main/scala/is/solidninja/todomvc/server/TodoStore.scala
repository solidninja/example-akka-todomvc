package is
package solidninja
package todomvc
package server

import java.util.UUID

import akka.actor.typed.ActorRef
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import is.solidninja.todomvc.protocol.TodoQueryResponse.NotFound
import is.solidninja.todomvc.protocol._
import is.solidninja.todomvc.server.TodoActorProtocol.{CommandReplyMessage, EventReply, QueryReply, QueryReplyMessage}

object TodoStore {
  type State = Map[UUID, Todo]
  type Command = TodoActorProtocol.MessageWithReplyTarget
  type Event = TodoEvent
  type Response = TodoActorProtocol.Reply

  /** Create a new store of Todos, that follows the TodoActorProtocol
    */
  def apply(): EventSourcedBehavior[Command, Event, State] =
    EventSourcedBehavior.withEnforcedReplies(
      persistenceId = PersistenceId("todo", "store"),
      emptyState = Map.empty[UUID, Todo],
      commandHandler = (commandHandler _),
      eventHandler = (eventHandler _)
    )

  private def commandHandler(
      state: State,
      command: Command
  ): ReplyEffect[Event, State] = command match {
    case CommandReplyMessage(subscriber, c) => updateCommandHandler(subscriber, state, c)
    case QueryReplyMessage(subscriber, q)   => queryHandler(subscriber, state, q)
  }

  private def updateCommandHandler(
      subscriber: ActorRef[Response],
      state: State,
      command: TodoCommand
  ): ReplyEffect[Event, State] = command match {
    case TodoCommand.AddTodo(todo) =>
      val event = TodoEvent.TodoUpdated(todo.withId(UUID.randomUUID()))
      Effect.persist(event).thenReply(subscriber)(_ => EventReply(event))
    case TodoCommand.CompleteTodo(id) =>
      state.get(id) match {
        case Some(todo) =>
          val updated = TodoEvent.TodoUpdated(todo.copy(completed = true))
          Effect.persist(updated).thenReply(subscriber)(_ => EventReply(updated))
        case None => Effect.none.thenReply(subscriber)(_ => QueryReply(NotFound))
      }
    case TodoCommand.UpdateTodo(todo) =>
      val updated = TodoEvent.TodoUpdated(todo)
      Effect.persist(updated).thenReply(subscriber)(_ => EventReply(updated))
  }

  private def queryHandler(subscriber: ActorRef[Response], state: State, query: TodoQuery): ReplyEffect[Event, State] =
    query match {
      case TodoQuery.GetTodos =>
        Effect.reply(subscriber)(QueryReply(TodoQueryResponse.FoundAll(state.values.toList)))
      case TodoQuery.GetTodo(id) =>
        Effect.reply(subscriber)(
          QueryReply(
            state
              .get(id)
              .fold[TodoQueryResponse](TodoQueryResponse.NotFound)(TodoQueryResponse.Found)
          )
        )
    }

  private def eventHandler(state: State, event: Event): State = event match {
    case TodoEvent.TodoUpdated(todo) => state.updated(todo.id, todo)
  }

}
