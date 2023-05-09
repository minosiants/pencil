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

package pencil

import cats.effect.{Async, Concurrent, Resource}
import com.comcast.ip4s.*
import fs2.io.net.tls.TLSContext
import fs2.io.net.{Network, Socket}
import org.typelevel.log4cats.Logger
import pencil.{Host => PHost}

import java.time.Instant
import java.util.UUID
import scala.Function.const
import data.*
import protocol.*

/** Smtp client
  */
trait Client[F[_]] {

  /** Sends `email` to a smtp server
    *
    * @param email
    *   \- email to be sent
    * @param es
    *   \- sender [[EmailSender]]
    * @return
    *   \- IO of [[Replies]] from smtp server
    */
  def send(email: Email): F[Replies]

}

object Client:
  def apply[F[_]](
      address: SocketAddress[Host] = SocketAddress(host"localhost", port"25"),
      credentials: Option[Credentials] = None
  )(
      tlsContext: TLSContext[F],
      logger: Logger[F]
  )(using A: Async[F], C: Concurrent[F], N: Network[F]): Client[F] =
    new Client[F] {
      val socket: Resource[F, Socket[F]] = N.client(address)

      def tlsSmtpSocket(s: Socket[F]): Resource[F, SmtpSocket[F]] =
        tlsContext.client(s).map { cs =>
          SmtpSocket.fromSocket(cs, logger)
        }

      override def send(email: Email): F[Replies] = {
        val sockets = for
          s <- socket
          tls <- tlsSmtpSocket(s)
        yield (s, tls)
        sockets.use { case (s, tls) =>
          val request = for
            _ <- Smtp.init[F]()
            rep <- Smtp.ehlo[F]()
            r <-
              if supportTLS(rep) then sendEmailViaTls(tls)
              else login(rep).flatMap(_ => sender)
          yield r

          request.run(
            Request(
              email,
              SmtpSocket.fromSocket(s, logger)
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
        rep.replies.exists(reply => reply.text.contains("AUTH") && reply.text.contains("LOGIN"))

      def sendEmailViaTls(
          tls: SmtpSocket[F]
      ): Smtp[F, Replies] =
        for
          _ <- Smtp.startTls[F]()
          r <- Smtp.local { (req: Request[F]) =>
            Request(
              req.email,
              tls
            )
          }(for
            rep <- Smtp.ehlo[F]()
            _ <- login(rep)
            r <- sender
          yield r)
        yield r

      def sender: Smtp[F, Replies] = Smtp.ask[F].flatMap { r =>
        r.email match {
          case Email(_, _, _, _, _, _, EmailType.Text) =>
            for
              _ <- Smtp.mail[F]()
              _ <- Smtp.rcpt[F]()
              _ <- Smtp.data[F]()
              _ <- Smtp.mainHeaders[F]()
              _ <- Smtp.emptyLine[F]()
              r <- Smtp.asciiBody[F]()
              _ <- Smtp.quit[F]()
            yield r

          case Email(_, _, _, _, _, _, EmailType.Mime(_, _)) =>
            for
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
            yield r
        }
      }
    }
