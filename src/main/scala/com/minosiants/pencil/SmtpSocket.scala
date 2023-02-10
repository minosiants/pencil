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

import cats._
import cats.effect.Sync
import cats.implicits._
import com.minosiants.pencil.data.Error
import com.minosiants.pencil.protocol.Command._
import com.minosiants.pencil.protocol._
import fs2.Chunk
import fs2.io.net.Socket
import org.typelevel.log4cats.Logger
import scodec.bits.BitVector
import scodec.codecs._
import scodec.{ Attempt, DecodeResult }

/**
  * Wraps [[Socket[IO]]] with smtp specific protocol
  */
trait SmtpSocket[F[_]] {

  /**
    * Reads [[Replies]] from smtp server
    */
  def read(): F[Replies]

  /**
    * Send [[Command]] to smtp server
    */
  def write(command: Command): F[Unit]
}

object SmtpSocket {
  def fromSocket[F[_]: Sync: MonadThrow](
      s: Socket[F],
      logger: Logger[F]
  ): SmtpSocket[F] = new SmtpSocket[F] {
    def bytesToReply(bytes: Array[Byte]): F[Replies] =
      Replies.codec.decode(BitVector(bytes)) match {
        case Attempt.Successful(DecodeResult(value, _)) =>
          logger.debug(s"Getting Replies: ${value.show}") *>
            Applicative[F].pure(value)

        case Attempt.Failure(cause) =>
          logger.debug(s" Getting Error: ${cause.messageWithContext}") *>
            Error.smtpError[F, Replies](cause.messageWithContext)
      }

    override def read(): F[Replies] =
      s.read(8192).flatMap {
        case Some(chunk) => bytesToReply(chunk.toArray)
        case None        => Error.smtpError[F, Replies]("Nothing to read")
      }

    override def write(command: Command): F[Unit] = {
      ascii.encode(command.show) match {
        case Attempt.Successful(value) =>
          logger.debug(s"Sending command: ${command.show}") *>
            s.write(Chunk.array(value.toByteArray))

        case Attempt.Failure(cause) =>
          Error.smtpError[F, Unit](cause.messageWithContext)
      }
    }
  }
}
