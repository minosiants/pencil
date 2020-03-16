/*
 * Copyright 2020 Kaspar Minosiants
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.minosiants.pencil

import cats.effect.{ ContextShift, IO, Resource }
import com.minosiants.pencil.data._
import com.minosiants.pencil.protocol._
import fs2.io.tcp.SocketGroup

import scala.concurrent.duration._

trait Client {
  def send[A](email: A)(implicit es: EmailSender[A]): IO[Replies]

}

trait EmailSender[A] {
  def send(email: A, socket: Resource[IO, SmtpSocket]): IO[Replies]
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
    )(implicit es: EmailSender[A]): IO[Replies] = {
      es.send(email, socket)
    }
  }

  implicit lazy val textEmailSender: EmailSender[AsciiEmail] =
    new EmailSender[AsciiEmail] {
      override def send(
          email: AsciiEmail,
          socket: Resource[IO, SmtpSocket]
      ): IO[Replies] =
        socket.use { s =>
          val sendProg = for {
            _ <- Smtp.init()
            _ <- Smtp.ehlo()
            _ <- Smtp.mail()
            _ <- Smtp.rcpt()
            _ <- Smtp.data()
            _ <- Smtp.mainHeaders()
            r <- Smtp.asciiBody()
            _ <- Smtp.quit()
          } yield r
          sendProg.run(Request(email, s))
        }
    }
  implicit lazy val mimeEmailSender: EmailSender[MimeEmail] =
    new EmailSender[MimeEmail] {
      override def send(
          email: MimeEmail,
          socket: Resource[IO, SmtpSocket]
      ): IO[Replies] = socket.use { s =>
        val sendProg = for {
          _ <- Smtp.init()
          _ <- Smtp.ehlo()
          _ <- Smtp.mail()
          _ <- Smtp.rcpt()
          _ <- Smtp.data()
          _ <- Smtp.mimeHeader()
          _ <- Smtp.mainHeaders()
          _ <- if (email.isMultipart) Smtp.multipart() else Smtp.pure(())
          _ <- Smtp.mimeBody()
          _ <- Smtp.attachments()
          r <- Smtp.endEmail()
          _ <- Smtp.quit()
        } yield r

        sendProg.run(Request(email, s))
      }
    }

}
