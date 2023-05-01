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
package protocol
import data.*
import cats.Show
import scodec.Codec

enum Command:
  case Ehlo(domain: String)
  case Mail(mailbox: Mailbox)
  case Rcpt(mailbox: Mailbox)
  case Vrfy(str: String)
  case Data
  case Rset
  case Noop
  case Quit
  case Text(txt: String)
  case AuthLogin
  case StartTls

object Command:
  val end = "\r\n"
  val endEmail: String = s"$end.$end"
  given Show[Command] = Show.show {
    case Ehlo(domain) => s"EHLO $domain$end"
    case Mail(Mailbox(localPart, domain)) =>
      s"MAIL FROM: <$localPart@$domain>$end"
    case Rcpt(Mailbox(localPart, domain)) =>
      s"RCPT TO: <$localPart@$domain>$end"
    case Data      => s"DATA$end"
    case Rset      => s"RSET$end"
    case Vrfy(str) => s"VRFY $str$end"
    case Noop      => s"NOOP$end"
    case Quit      => s"QUIT$end"
    case Text(txt) => s"$txt"
    case AuthLogin => s"AUTH LOGIN$end"
    case StartTls  => s"STARTTLS$end"
  }
  given Codec[Command] = CommandCodec()
