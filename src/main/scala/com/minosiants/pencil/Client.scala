package com.minosiants.pencil

import cats.effect.{ ContextShift, IO, Resource }
import com.minosiants.pencil.data._
import com.minosiants.pencil.protocol._
import fs2.io.tcp.SocketGroup

import scala.concurrent.duration._

trait Client {
  def send[A](email: A)(implicit es: EmailSender[A]): IO[List[Replies]]

}

trait EmailSender[A] {
  def send(email: A, socket: Resource[IO, SmtpSocket]): IO[List[Replies]]
}

object Client {
  def apply(
      host: String,
      port: Int = 25,
      readTimeout: FiniteDuration = 5.minutes,
      writeTimeout: FiniteDuration = 5.minutes
  )(sg: SocketGroup)(implicit cs: ContextShift[IO]): Client = new Client {

    lazy val socket: Resource[IO, SmtpSocket] =
      SmtpSocket(host, port, readTimeout, writeTimeout, sg)

    override def send[A](
        email: A
    )(implicit es: EmailSender[A]): IO[List[Replies]] = {
      es.send(email, socket)
    }
  }

  implicit lazy val textEmailSender: EmailSender[AsciiEmail] =
    new EmailSender[AsciiEmail] {
      override def send(
          email: AsciiEmail,
          socket: Resource[IO, SmtpSocket]
      ): IO[List[Replies]] = {
        socket.use { s =>
          val sendProg = for {
            i <- Smtp.init()
            e <- Smtp.ehlo()
            m <- Smtp.mail()
            r <- Smtp.rcpt()
            d <- Smtp.data()
            _ <- Smtp.mainHeaders()
            b <- Smtp.asciiBody()
            q <- Smtp.quit()
          } yield q :: b.toList ++ (d :: r ++ (m :: e :: i :: Nil))
          sendProg.run(Request(email, s))
        }
      }

      implicit lazy val mimeEmailSender: EmailSender[MimeEmail] =
        new EmailSender[MimeEmail] {
          override def send(
              email: MimeEmail,
              socket: Resource[IO, SmtpSocket]
          ): IO[List[Replies]] = {

            ???
          }
        }
    }

}
