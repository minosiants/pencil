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

import cats.Show
import com.minosiants.pencil.data.Mailbox
import scodec.Codec

sealed trait Command extends Product with Serializable

final case class Ehlo(domain: String)   extends Command
final case class Mail(mailbox: Mailbox) extends Command
final case class Rcpt(mailbox: Mailbox) extends Command
final case class Vrfy(str: String)      extends Command
case object Data                        extends Command
case object Rset                        extends Command
case object Noop                        extends Command
case object Quit                        extends Command
case class Text(txt: String)            extends Command

object Command {

  implicit lazy val CommandShow: Show[Command] = Show.show {
    case Ehlo(domain) => s"EHLO $domain $end"
    case Mail(Mailbox(localPart, domain)) =>
      s"MAIL FROM: <$localPart@$domain> $end"
    case Rcpt(Mailbox(localPart, domain)) =>
      s"RCPT TO: <$localPart@$domain> $end"
    case Data      => s"DATA $end"
    case Rset      => s"RSET $end"
    case Vrfy(str) => s"VRFY $str $end"
    case Noop      => s"NOOP $end"
    case Quit      => s"QUIT $end"
    case Text(txt) => s"$txt"
  }

  val end      = "\r\n"
  val endEmail = s"$end.$end"

  lazy val codec: Codec[Command] = CommandCodec()
}
