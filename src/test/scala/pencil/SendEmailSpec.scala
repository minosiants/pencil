/*
package pencil

import cats.effect.IO
import fs2.io.net.Network
import cats.syntax.either.*
import cats.effect.unsafe.implicits.global
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.Client as HClient
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto.*
import org.http4s.EntityDecoder
import pencil.data.{Email, Mailbox}
import org.http4s.circe.*
import pencil.protocol.Replies
class SendEmailSpec extends MailServerSpec {
  sequential
  "email" should {
    "send mime email" in {
      val email = SmtpSpec2.mimeEmail
      val message = EmberClientBuilder
        .default[IO]
        .build
        .use { httpClient =>
          for
            _ <- sendEmail(SmtpSpec2.mimeEmail)
            messages <- httpClient.expect[Messages](
              s"""http://localhost:${container.httpPort}/api/v1/messages"""
            )
            id = messages.messages.head.ID
            message <- httpClient.expect[Message2](
              s"""http://localhost:${container.httpPort}/api/v1/message/$id"""
            )
          yield message
        }
        .unsafeRunSync()

      message.Bcc.map(_.Address) ==== email.bcc.toList.flatMap(_.toList.map(_.address))
      message.Cc.map(_.Address) ==== email.cc.toList.flatMap(_.toList.map(_.address))
      message.To.map(_.Address) ==== email.to.toList.map(_.address)
      message.From.Address ==== email.from.address
      message.Subject ==== email.subject.get.asString
      message.Text ==== email.body.get.value
    }
  }

  def sendEmail(email: Email): IO[Replies] = for
    tls <- Network[IO].tlsContext.system
    smtpClient = Client[IO](container.socketAddress(), Some(container.credentials))(tls, logger)
    response <- smtpClient.send(email)
  yield response
}

object SendEmailSpec {}

final case class MailBox(Name: String, Address: String)

final case class Message(
    ID: String,
    From: MailBox,
    To: List[MailBox],
    Cc: List[MailBox],
    Bcc: List[MailBox],
    Subject: String,
    Attachments: Int
)
final case class Message2(
    ID: String,
    From: MailBox,
    To: List[MailBox],
    Cc: List[MailBox],
    Bcc: List[MailBox],
    Subject: String,
    Text: String,
    HTML: String
)

object Message2:
  given EntityDecoder[IO, Message2] = jsonOf[IO, Message2]

object Message:
  given mbDecoder: Decoder[Mailbox] = Decoder.decodeString.map(Mailbox.unsafeFromString)
  given EntityDecoder[IO, MailBox] = jsonOf[IO, MailBox]
  given EntityDecoder[IO, Message] = jsonOf[IO, Message]

final case class Messages(messages: List[Message])
object Messages:
  given EntityDecoder[IO, Messages] = jsonOf[IO, Messages]
*/