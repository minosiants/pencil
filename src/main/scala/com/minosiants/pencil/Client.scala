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

import java.net.InetSocketAddress

import cats.effect.{ ContextShift, IO, Resource }
import com.minosiants.pencil.data._
import com.minosiants.pencil.protocol._
import com.minosiants.pencil.data.Email.{ MimeEmail, TextEmail }
import fs2.io.tcp.{ Socket, SocketGroup }

import scala.concurrent.duration._
import fs2.io.tls.TLSContext
import cats.syntax.flatMap._
import Function.const
import cats.effect.Blocker

/**
  * Smtp client
  *
  */
trait Client {

  /**
    * Sends `email` to a smtp server
    *
    * @param email - email to be sent
    * @param es - sender [[EmailSender]]
    * @return - IO of [[Replies]] from smtp server
    */
  def send(email: Email): IO[Replies]

}

object Client {
  def apply(
      host: String = "localhost",
      port: Int = 25,
      credentials: Option[Credentials] = None,
      readTimeout: FiniteDuration = 5.minutes,
      writeTimeout: FiniteDuration = 5.minutes,
  )(blocker: Blocker, sg: SocketGroup, tlsContext: TLSContext)(
      implicit cs: ContextShift[IO]
  ): Client = new Client {

    lazy val socket: Resource[IO, Socket[IO]] =
      sg.client[IO](new InetSocketAddress(host, port))

    lazy val tlsSocket: Socket[IO] => Resource[IO, SmtpSocket] =
      (s: Socket[IO]) =>
        tlsContext
          .client[IO](s)
          .map(SmtpSocket.fromSocket(_, readTimeout, writeTimeout))

    override def send(
        email: Email
    ): IO[Replies] = {

      socket.use { s =>
        tlsSocket(s).use { tls =>
          (for {
            _   <- Smtp.init()
            rep <- Smtp.ehlo()
            r <- if (supportTLS(rep)) sendEmailViaTls(tls)
            else login(rep) >> sender
          } yield r).run(
            Request(
              email,
              SmtpSocket.fromSocket(s, readTimeout, writeTimeout),
              blocker
            )
          )

        }
      }
    }

    def login(rep: Replies): Smtp[Unit] =
      credentials
        .filter(const(supportLogin(rep)))
        .fold(Smtp.pure(()))(Smtp.login)

    def supportTLS(rep: Replies): Boolean =
      rep.replies.exists(r => r.text.contains("STARTTLS"))

    def supportLogin(rep: Replies): Boolean =
      rep.replies.exists(_.text.contains("AUTH LOGIN"))

    def sendEmailViaTls(
        tls: SmtpSocket
    ): Smtp[Replies] =
      for {
        _ <- Smtp.startTls()
        r <- Smtp.local(req => Request(req.email, tls, req.blocker))(for {
          rep <- Smtp.ehlo()
          _   <- login(rep)
          r   <- sender
        } yield r)
      } yield r
  }

  private def sender: Smtp[Replies] = Smtp.ask.flatMap{r => 
    r.email match {
      case TextEmail(_, _,_, _, _, _) => 
        for {
          _ <- Smtp.mail()
          _ <- Smtp.rcpt()
          _ <- Smtp.data()
          _ <- Smtp.mainHeaders()
          r <- Smtp.asciiBody()
          _ <- Smtp.quit()
        } yield r
      case MimeEmail(_, _, _, _, _, _, _, _) =>
        for {
          _ <- Smtp.mail()
          _ <- Smtp.rcpt()
          _ <- Smtp.data()
          _ <- Smtp.mimeHeader()
          _ <- Smtp.mainHeaders()
          _ <- Smtp.multipart()
          _ <- Smtp.mimeBody()
          _ <- Smtp.attachments()
          r <- Smtp.endEmail()
          _ <- Smtp.quit()
        } yield r
    }
  }

}
