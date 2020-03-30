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
package protocol

import com.minosiants.pencil.data.{ Error, Mailbox }
import Command._
import scodec.bits.{ BitVector, ByteVector }
import scodec.codecs._
import scodec.{ Attempt, Codec, DecodeResult, Err, SizeBound }

final case class CommandCodec() extends Codec[Command] {

  override def decode(bits: BitVector): Attempt[DecodeResult[Command]] = {
    limitedSizeBits(4 * 8, ascii).decode(bits).flatMap {
      case DecodeResult(cmd, rest) =>
        cmd match {
          case "EHLO" =>
            ascii.decode(stripEND(rest)).map {
              case DecodeResult(domain, _) =>
                DecodeResult(Ehlo(domain.trim), BitVector.empty)
            }
          case "MAIL" =>
            MailboxCodec.codec.decode(rest).map {
              case DecodeResult(email, _) =>
                DecodeResult(Mail(email), BitVector.empty)
            }
          case "RCPT" =>
            MailboxCodec.codec.decode(rest.drop(4 * 8)).map {
              case DecodeResult(email, _) =>
                DecodeResult(Rcpt(email), BitVector.empty)
            }
          case "DATA" => Attempt.successful(DecodeResult(Data, BitVector.empty))
          case "QUIT" => Attempt.successful(DecodeResult(Quit, BitVector.empty))
          case "RSET" => Attempt.successful(DecodeResult(Rset, BitVector.empty))
          case "NOOP" => Attempt.successful(DecodeResult(Noop, BitVector.empty))
          case "VRFY" =>
            ascii.decode(stripEND(rest)).map {
              case DecodeResult(txt, _) =>
                DecodeResult(Vrfy(txt.trim), BitVector.empty)
            }
          case "AUTH" =>
            Attempt.successful(DecodeResult(AuthLogin, BitVector.empty))
          case "STAR" =>
            Attempt.successful(DecodeResult(StartTls, BitVector.empty))
          case _ =>
            ascii.decode(bits).map {
              case DecodeResult(txt, _) =>
                DecodeResult(Text(txt), BitVector.empty)
            }
        }
    }

  }

  override def encode(v: Command): Attempt[BitVector] =
    Attempt.successful(Command.CommandShow.show(v).toBitVector)

  override def sizeBound: SizeBound = SizeBound.unknown

  private val END = Command.end.toBitVector
  private def stripEND(bits: BitVector) = {
    bits.take(bits.size - END.size)
  }

}

final case class MailboxCodec() extends Codec[Mailbox] {
  private val `<` = ByteVector("<".getBytes)
  private val `>` = ByteVector(">".getBytes)

  private def extractEmail(bits: BitVector): Attempt[BitVector] = {
    val bytes = bits.toByteVector
    val from  = bytes.indexOfSlice(`<`)
    val to    = bytes.indexOfSlice(`>`)
    if (from < 0 || to < 0)
      Attempt.failure(Err("email does not included into '<' '>'"))
    else
      Attempt.successful(bytes.slice(from + `<`.size, to).bits)

  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Mailbox]] =
    extractEmail(bits).flatMap(ascii.decode(_).flatMap {
      case DecodeResult(box, remainder) =>
        Mailbox.fromString(box) match {
          case Right(mb) =>
            Attempt.successful(DecodeResult(mb, remainder))
          case Left(error) =>
            Attempt.failure(Err(Error.errorShow.show(error)))
        }
    })

  override def encode(mb: Mailbox): Attempt[BitVector] =
    Attempt.successful(Mailbox.mailboxShow.show(mb).toBitVector)

  override def sizeBound: SizeBound = SizeBound.unknown
}

object MailboxCodec {
  val codec = MailboxCodec()
}
