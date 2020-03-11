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
