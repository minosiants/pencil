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
import java.time.Instant
import java.util.UUID

import cats.effect._
import com.minosiants.pencil.data.Email.{ MimeEmail, TextEmail }
import com.minosiants.pencil.data._
import com.minosiants.pencil.protocol._
import fs2.io.tcp.{ Socket, SocketGroup }
import fs2.io.tls.TLSContext
import org.typelevel.log4cats.Logger

import scala.Function.const
import scala.concurrent.duration._

/**
  * Smtp client
  *
  */
trait Client[F[_]] {

  /**
    * Sends `email` to a smtp server
    *
    * @param email - email to be sent
    * @param es - sender [[EmailSender]]
    * @return - IO of [[Replies]] from smtp server
    */
  def send(email: Email): F[Replies]

}

object Client {
  def apply[F[_]: Concurrent: ContextShift](
      host: String = "localhost",
      port: Int = 25,
      credentials: Option[Credentials] = None,
      readTimeout: FiniteDuration = 5.minutes,
      writeTimeout: FiniteDuration = 5.minutes
  )(
      blocker: Blocker,
      sg: SocketGroup,
      tlsContext: TLSContext,
      logger: Logger[F]
  ): Client[F] =
    new Client[F] {
      val socket: Resource[F, Socket[F]] =
        sg.client[F](new InetSocketAddress(host, port))

      def tlsSmtpSocket(s: Socket[F]): Resource[F, SmtpSocket[F]] =
        tlsContext.client(s).map { cs =>
          SmtpSocket.fromSocket(cs, logger, readTimeout, writeTimeout)
        }

      override def send(
          email: Email
      ): F[Replies] = {
        val sockets = for {
          s   <- socket
          tls <- tlsSmtpSocket(s)
        } yield (s, tls)

        sockets.use {
          case (s, tls) =>
            val request = for {
              _   <- Smtp.init[F]()
              rep <- Smtp.ehlo[F]()
              r <- if (supportTLS(rep)) sendEmailViaTls(tls)
              else login(rep).flatMap(_ => sender)
            } yield r

            request.run(
              Request(
                email,
                SmtpSocket.fromSocket(s, logger, readTimeout, writeTimeout),
                blocker,
                Host.local(),
                Instant.now(),
                UUID.randomUUID().toString
              )
            )
        }
      }

      def login(rep: Replies): Smtp[F, Unit] =
        credentials
          .filter(const(supportLogin(rep)))
          .fold(Smtp.unit)(Smtp.login[F])

      def supportTLS(rep: Replies): Boolean =
        rep.replies.exists(r => r.text.contains("STARTTLS"))

      def supportLogin(rep: Replies): Boolean =
        rep.replies.exists(
          reply => reply.text.contains("AUTH") && reply.text.contains("LOGIN")
        )

      def sendEmailViaTls(
          tls: SmtpSocket[F]
      ): Smtp[F, Replies] =
        for {
          _ <- Smtp.startTls[F]()
          r <- Smtp.local { req: Request[F] =>
            Request(
              req.email,
              tls,
              req.blocker,
              Host.local(),
              Instant.now(),
              UUID.randomUUID().toString
            )
          }(for {
            rep <- Smtp.ehlo[F]()
            _   <- login(rep)
            r   <- sender
          } yield r)
        } yield r

      def sender: Smtp[F, Replies] = Smtp.ask[F].flatMap { r =>
        r.email match {
          case TextEmail(_, _, _, _, _, _) =>
            for {
              _ <- Smtp.mail[F]()
              _ <- Smtp.rcpt[F]()
              _ <- Smtp.data[F]()
              _ <- Smtp.mainHeaders[F]()
              _ <- Smtp.emptyLine[F]()
              r <- Smtp.asciiBody[F]()
              _ <- Smtp.quit[F]()
            } yield r

          case MimeEmail(_, _, _, _, _, _, _, _) =>
            for {
              _ <- Smtp.mail[F]()
              _ <- Smtp.rcpt[F]()
              _ <- Smtp.data[F]()
              _ <- Smtp.mimeHeader[F]()
              _ <- Smtp.mainHeaders[F]()
              _ <- Smtp.multipart[F]()
              _ <- Smtp.emptyLine[F]()
              _ <- Smtp.mimeBody[F]()
              _ <- Smtp.attachments[F]()
              r <- Smtp.endEmail[F]()
              _ <- Smtp.quit[F]()
            } yield r
        }
      }
    }
}
