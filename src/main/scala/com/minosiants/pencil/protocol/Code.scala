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


enum Code(value:Int, description: String):
  case `214` extends Code(214, "Help message")
  case `211` extends Code(211, "System status, or system help reply")
  case `220` extends Code(220, "Service ready")
  case `221` extends Code(221, "Service closing transmission channel")
  case `235` extends Code(235, "2.7.0 Authentication successful")
  case `250` extends Code(250, "Requested mail action okay, completed")
  case `251` extends Code(251, "User not local; will forward to <forward-path>")
  case `252` extends Code(252, "Cannot VRFY user, but will accept message and attempt delivery")
  case `334` extends Code(334, "Server challenge response")
  case `354` extends Code(354, "Start mail input; end with <CRLF>.<CRLF")
  case `421` extends Code(421, "<domain> Service not available, closing transmission channel")
  case `450` extends Code(450, "Requested mail action not taken: mailbox unavailable")
  case `451` extends Code(451, "Requested action aborted: local error in processing")
  case `452` extends Code(452, "Requested action not taken: insufficient system storage")
  case `455` extends Code(455, "Server unable to accommodate parameters")
  case `500` extends Code(500, "Syntax error, command unrecognized")
  case `501` extends Code(501, "Syntax error in parameters or arguments")
  case `502` extends Code(502, "Command not implemented")
  case `503` extends Code(503, "Bad sequence of commands")
  case `504` extends Code(504, "Command parameter not implemented")
  case `530` extends Code(530, "SMTP authentication is required")
  case `534` extends Code(534, "Incorrect authentication data")
  case `535` extends Code(535, "Incorrect authentication data")
  case `550` extends Code(550, "Requested action not taken: mailbox unavailable")
  case `551` extends Code(551, "User not local; please try <forward-path>")
  case `552` extends Code(552, "Requested mail action aborted: exceeded storage allocation")
  case `553` extends Code(553, "Requested action not taken: mailbox name not allow")
  case `554` extends Code(554, "Transaction failed")
  case `555` extends Code(555, "MAIL FROM/RCPT TO parameters not recognized or not implemented")
  def success: Boolean = value < 400


object Code:

  val v = Code.values
    def code(value: Int): Option[Code] = Code.values.find(_.value == value)
    given Codec[Code] = Codec[Code](
      (value: Code) => ascii.encode(value.toString),
      (bits: BitVector) => {
        limitedSizeBits(3 * 8, ascii).decode(bits) match {
          case Successful(DecodeResult(code, rest)) =>
            Attempt.fromOption(
              Try(code.toInt).toOption
                .flatMap(Code.code)
                .map(c => DecodeResult(c, rest)),
              Err(s"$code code does not exist")
            )
          case Attempt.Failure(cause) => Attempt.failure(cause)
        }
      }
    )



