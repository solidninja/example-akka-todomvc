package is
package solidninja
package todomvc
package server

import java.io.NotSerializableException

import akka.actor.ExtendedActorSystem
import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem}
import akka.serialization.SerializerWithStringManifest
import io.circe.generic.semiauto._
import io.circe.jawn.parse
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder}
import is.solidninja.todomvc.protocol._
import is.solidninja.todomvc.server.TodoActorProtocol.{MessageWithReplyTarget, Reply}

object TodoActorProtocol extends JsonProtocol {
  sealed trait Reply

  final case class EventReply(event: TodoEvent) extends Reply
  final case class QueryReply(queryResponse: TodoQueryResponse) extends Reply

  sealed trait MessageWithReplyTarget

  final case class CommandReplyMessage(actorRef: ActorRef[Reply], message: TodoCommand) extends MessageWithReplyTarget
  final case class QueryReplyMessage(actorRef: ActorRef[Reply], message: TodoQuery) extends MessageWithReplyTarget
}

trait AkkaProtocol {
  implicit def actorRefEncoder[T](implicit system: ActorSystem[_]): Encoder[ActorRef[T]] =
    Encoder[String]
      .contramap(ActorRefResolver(system).toSerializationFormat(_))

  implicit def actorRefDecoder[T](implicit system: ActorSystem[_]): Decoder[ActorRef[T]] =
    Decoder[String]
      .map(ActorRefResolver(system).resolveActorRef[T](_))
}

trait TodoActorJsonProtocol extends JsonProtocol with AkkaProtocol {
  implicit val replyCodec: Codec[Reply] = deriveCodec
  implicit def messageWithReplyTargetCodec(implicit as: ActorSystem[_]): Codec[MessageWithReplyTarget] = {
    val _ = as // get rid of unused warning
    deriveCodec
  }
}

class TodoActorProtocolSerializer(as: ExtendedActorSystem)
    extends SerializerWithStringManifest
    with TodoActorJsonProtocol {
  import akka.actor.typed.scaladsl.adapter._

  implicit val actorSystem: ActorSystem[_] = as.toTyped

  override def identifier: Int = 123456

  override def manifest(o: AnyRef): String = o match {
    case _: Reply                  => "reply"
    case _: MessageWithReplyTarget => "message-with-reply"
    case _: TodoEvent              => "event"
    case _: TodoQuery              => "query"
    case _: TodoCommand            => "command"
    case _: TodoQueryResponse      => "query-response"
    case other                     => throw new NotSerializableException(s"Could not determine manifest for $other")
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case r: Reply                  => r.asJson.noSpacesSortKeys.getBytes
    case m: MessageWithReplyTarget => m.asJson.noSpacesSortKeys.getBytes
    case e: TodoEvent              => e.asJson.noSpacesSortKeys.getBytes
    case q: TodoQuery              => q.asJson.noSpacesSortKeys.getBytes
    case c: TodoCommand            => c.asJson.noSpacesSortKeys.getBytes
    case r: TodoQueryResponse      => r.asJson.noSpacesSortKeys.getBytes
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case "reply"              => parse(new String(bytes)).flatMap(_.as[Reply]).toTry.get
    case "message-with-reply" => parse(new String(bytes)).flatMap(_.as[MessageWithReplyTarget]).toTry.get
    case "event"              => parse(new String(bytes)).flatMap(_.as[TodoEvent]).toTry.get
    case "query"              => parse(new String(bytes)).flatMap(_.as[TodoQuery]).toTry.get
    case "command"            => parse(new String(bytes)).flatMap(_.as[TodoCommand]).toTry.get
    case "query-response"     => parse(new String(bytes)).flatMap(_.as[TodoQueryResponse]).toTry.get
  }

}
