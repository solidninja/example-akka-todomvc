package is
package solidninja
package todomvc
package protocol

import io.circe._
import io.circe.generic.semiauto._

trait JsonProtocol {
  implicit val todoCodec: Codec[Todo] = deriveCodec
  implicit val newTodoCodec: Codec[NewTodo] = deriveCodec

  implicit val todoCommandCodec: Codec[TodoCommand] = deriveCodec
  implicit val todoEventCodec: Codec[TodoEvent] = deriveCodec
  implicit val todoQueryCodec: Codec[TodoQuery] = deriveCodec
  implicit val todoQueryResponseCodec: Codec[TodoQueryResponse] = deriveCodec
}

object JsonProtocol extends JsonProtocol
