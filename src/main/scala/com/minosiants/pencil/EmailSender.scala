package com.minosiants.pencil

import cats.effect.{ IO, Resource }
import com.minosiants.pencil.data.{ Credentials, Email }
import com.minosiants.pencil.data.Email.{ MimeEmail, TextEmail }
import com.minosiants.pencil.protocol.Replies

trait EmailSender[A <: Email] {
  def send(
      email: A,
      credentials: Option[Credentials],
      socket: Resource[IO, SmtpSocket],
      tlsSocket: Resource[IO, SmtpSocket]
  ): IO[Replies] =
    socket.use { s =>
      tlsSocket.use { tls =>
        (for {
          _   <- Smtp.init()
          rep <- Smtp.ehlo()
          r <- if (supportTLS(rep)) sendEmailViaTls(credentials, tls)
          else sendEmail(credentials)
        } yield r).run(SmtpRequest(email, s))

      }
    }

  def supportTLS(rep: Replies): Boolean = {
    rep.replies.exists(r => r.text.contains("STARTTLS"))
  }
  def sendEmailViaTls(
      credentials: Option[Credentials],
      tls: SmtpSocket
  ): Smtp[Replies] =
    Smtp.local(req => SmtpRequest(req.email, tls))(for {
      _ <- Smtp.ehlo()
      r <- sendEmail(credentials)
    } yield r)

  def sendEmail(credentials: Option[Credentials]): Smtp[Replies]
}

object EmailSender {
  implicit lazy val textEmailSender: EmailSender[TextEmail] = TextEmailSender()
  implicit lazy val mimeEmailSender: EmailSender[MimeEmail] = MimeEmailSender()

}
