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
import fs2.io.tcp.{ Socket, SocketGroup }

import scala.concurrent.duration._
import fs2.io.tls.{ TLSContext, TLSSocket }

trait Client {
  def send[A<:Email](email: A)(implicit es: EmailSender[A]): IO[Replies]

}

object Client {
  def apply(
      host: String,
      port: Int = 25,
      credentials: Option[Credentials] = None,
      tlsContext: TLSContext,
      readTimeout: FiniteDuration = 5.minutes,
      writeTimeout: FiniteDuration = 5.minutes
  )(sg: SocketGroup)(implicit cs: ContextShift[IO]): Client = new Client {

    lazy val socket: Resource[IO, Socket[IO]] =
      sg.client[IO](new InetSocketAddress(host, port))

    lazy val smtpSocket =
      socket.map(SmtpSocket.fromSocket(_, readTimeout, writeTimeout))

    lazy val tlsSmtpSocket: Resource[IO, SmtpSocket] =
      socket
        .flatMap(tlsContext.client[IO](_))
        .map(SmtpSocket.fromSocket(_, readTimeout, writeTimeout))

    override def send[A](
        email: A
    )(implicit es: EmailSender[A]): IO[Replies] = {
      es.send(email, credentials, smtpSocket, tlsSmtpSocket)
    }
  }

}
