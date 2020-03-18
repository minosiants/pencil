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
import java.nio.file.Paths

import cats.effect._
//import cats.implicits._
import com.minosiants.pencil.Client._
import com.minosiants.pencil.data._
import fs2.io.tcp.SocketGroup
import Email._
import fs2.Chunk
import fs2.io.tls.TLSContext
import scodec.{ Attempt, DecodeResult }
import scodec.bits.BitVector
import scodec.codecs._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val r = Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          sg.client[IO](new InetSocketAddress("localhost", 25)).use { socket =>
            TLSContext.system[IO](blocker).flatMap { tls =>
              tls.client(socket).use { client =>
                client.read(1024).flatMap(f)

              }

            }

          }
        }
      }

    r.attempt.flatMap {
      case Right(v) =>
        println(">>>" + v)
        IO(ExitCode.Success)
      case Left(e) =>
        e.printStackTrace()
        IO(ExitCode.Error)
    }
  }

  def f(chunks: Option[Chunk[Byte]]) = {
    chunks match {
      case Some(chunk) =>
        utf8.decode(BitVector.view(chunk.toArray)) match {
          case Attempt.Successful(DecodeResult(value, _)) => IO(value)
          case Attempt.Failure(cause) =>
            data.Error.smtpError(cause.messageWithContext)
        }
      case None => data.Error.smtpError("Nothing to read")
    }
  }

}
