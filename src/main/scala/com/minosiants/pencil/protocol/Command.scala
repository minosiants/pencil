package com.minosiants.pencil
package protocol

import cats.Show

sealed trait Command extends Product with Serializable

case class Maibox(value: String) //TODO

final case class Ehlo(domain: String)  extends Command
final case class Mail(mailbox: Maibox) extends Command

object Command {

  implicit lazy val CommandShow: Show[Command] = Show.show {
    case Ehlo(domain)      => s"EHLO $domain $end"
    case Mail(Maibox(box)) => s"MAIL $box $end"
  }

  val end = "\r\n"
}
