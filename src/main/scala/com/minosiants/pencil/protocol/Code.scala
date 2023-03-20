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

package com.minosiants
package pencil.protocol

import scodec.Attempt.Successful
import scodec.bits.BitVector
import scodec.codecs.{ascii, limitedSizeBits}
import scodec.{Attempt, Codec, DecodeResult, Err}

import scala.util.Try

final case class Code(value: Int, description: String)
    extends Product
    with Serializable {
  def success: Boolean = value < 400
}

object Code {

  def code(value: Int): Option[Code] = codes.find(_.value == value)

  val `214`: Code = Code(214, "Help message")
  val `211`: Code = Code(211, "System status, or system help reply")
  val `220`: Code = Code(220, "Service ready")
  val `221`: Code = Code(221, "Service closing transmission channel")
  val `235`: Code = Code(235, "2.7.0 Authentication successful")
  val `250`: Code = Code(250, "Requested mail action okay, completed")
  val `251`: Code = Code(251, "User not local; will forward to <forward-path>")
  val `252`: Code =
    Code(252, "Cannot VRFY user, but will accept message and attempt delivery")
  val `334`: Code = Code(334, "Server challenge response")
  val `354`: Code = Code(354, "Start mail input; end with <CRLF>.<CRLF")
  val `421`: Code =
    Code(421, "<domain> Service not available, closing transmission channel")
  val `450`: Code =
    Code(450, "Requested mail action not taken: mailbox unavailable")
  val `451`: Code =
    Code(451, "Requested action aborted: local error in processing")
  val `452`: Code =
    Code(452, "Requested action not taken: insufficient system storage")
  val `455`: Code = Code(455, "Server unable to accommodate parameters")
  val `500`: Code = Code(500, "Syntax error, command unrecognized")
  val `501`: Code = Code(501, "Syntax error in parameters or arguments")
  val `502`: Code = Code(502, "Command not implemented")
  val `503`: Code = Code(503, "Bad sequence of commands")
  val `504`: Code = Code(504, "Command parameter not implemented")
  val `530`: Code = Code(530, "SMTP authentication is required")
  val `534`: Code = Code(534, "Incorrect authentication data")
  val `535`: Code = Code(535, "Incorrect authentication data")
  val `550`: Code = Code(550, "Requested action not taken: mailbox unavailable")
  val `551`: Code = Code(551, "User not local; please try <forward-path>")
  val `552`: Code =
    Code(552, "Requested mail action aborted: exceeded storage allocation")
  val `553`: Code =
    Code(553, "Requested action not taken: mailbox name not allow")
  val `554`: Code = Code(554, "Transaction failed")
  val `555`: Code =
    Code(555, "MAIL FROM/RCPT TO parameters not recognized or not implemented")

  val codes: List[Code] = List(
    `214`,
    `211`,
    `220`,
    `221`,
    `235`,
    `250`,
    `251`,
    `252`,
    `334`,
    `354`,
    `421`,
    `450`,
    `451`,
    `452`,
    `455`,
    `500`,
    `501`,
    `502`,
    `503`,
    `504`,
    `530`,
    `534`,
    `535`,
    `550`,
    `551`,
    `552`,
    `553`,
    `554`,
    `555`
  )

  implicit lazy val codec: Codec[Code] = Codec[Code](
    (value: Code) => ascii.encode(value.value.toString),
    (bits: BitVector) => {
      limitedSizeBits(3 * 8, ascii).decode(bits) match {
        case Successful(DecodeResult(code, rest)) =>
          Attempt.fromOption(
            Try(code.toInt).toOption
              .flatMap(Code.code)
              .map(c => DecodeResult(c, rest)),
            Err(s"${code} code does not exist")
          )
        case Attempt.Failure(cause) => Attempt.failure(cause)
      }
    }
  )
}
