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

import cats.effect._
import cats.syntax.show._
import com.minosiants.pencil.protocol.Command._
import com.minosiants.pencil.protocol._
import fs2.Chunk
import fs2.io.tcp.{ Socket, SocketGroup }
import scodec.bits.BitVector
import scodec.codecs._
import scodec.{ Attempt, DecodeResult }

import scala.concurrent.duration.FiniteDuration

trait SmtpSocket {

  def read(): IO[Replies]
  def write(command: Command): IO[Unit]
}

object SmtpSocket {

  def bytesToReply(bytes: Array[Byte]): IO[Replies] = {
    Reply.repliesCodec.decode(BitVector(bytes)) match {
      case Attempt.Successful(DecodeResult(value, _)) => IO(value)
      case Attempt.Failure(cause) =>
        data.Error.smtpError(cause.messageWithContext)
    }
  }

  def fromSocket(
      s: Socket[IO],
      readTimeout: FiniteDuration,
      writeTimeout: FiniteDuration
  ): SmtpSocket = new SmtpSocket {

    override def read(): IO[Replies] =
      s.read(8192, Some(readTimeout)).flatMap {
        case Some(chunk) => bytesToReply(chunk.toArray)
        case None        => data.Error.smtpError("Nothing to read")
      }

    override def write(command: Command): IO[Unit] = {
      ascii.encode(command.show) match {
        case Attempt.Successful(value) =>
          s.write(Chunk.array(value.toByteArray), Some(writeTimeout))
        case Attempt.Failure(cause) =>
          data.Error.smtpError(cause.messageWithContext)
      }

    }
  }
// default timeout should be 5 min
  def apply(
      host: String,
      port: Int,
      readTimeout: FiniteDuration,
      writeTimeout: FiniteDuration,
      sg: SocketGroup
  )(implicit cs: ContextShift[IO]): Resource[IO, SmtpSocket] = {
    sg.client[IO](new InetSocketAddress(host, port))
      .map(fromSocket(_, readTimeout, writeTimeout))
  }

  def apply(
      address: InetSocketAddress,
      readTimeout: FiniteDuration,
      writeTimeout: FiniteDuration,
      sg: SocketGroup
  )(implicit cs: ContextShift[IO]): Resource[IO, SmtpSocket] = {
    sg.client[IO](address)
      .map(fromSocket(_, readTimeout, writeTimeout))
  }
}
