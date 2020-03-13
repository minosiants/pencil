package com.minosiants.pencil
package data

import cats.Show

final case class Mailbox(localPart: String, domain: String)
    extends Product
    with Serializable {

  def address: String = s"$localPart@$domain"
}

object Mailbox {

  def fromString(mailbox: String): Either[Error, Mailbox] =
    MailboxParser.parse(mailbox)
  def unsafeFromString(mailbox: String): Mailbox =
    fromString(mailbox).fold(throw _, identity)

  implicit val mailboxShow: Show[Mailbox] =
    Show.show[Mailbox](mb => s"<${mb.address}>")

}
