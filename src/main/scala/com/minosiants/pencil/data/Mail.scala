package com.minosiants.pencil
package data

import java.io.File
import java.math.BigInteger

import protocol._
import Body._

final case class From(box: Mailbox) extends Product with Serializable
final case class To(boxes: List[Mailbox]) extends Product with Serializable {
  def +(mailbox: Mailbox) = this.copy(boxes :+ mailbox)
}
final case class Cc(boxes: List[Mailbox]) extends Product with Serializable
final case class Bcc(boxes:List[Mailbox]) extends Product with Serializable
final case class Subject(value: String)   extends Product with Serializable



final case class Attachment(file:File, contentType:ContentType) extends Product with Serializable


object To {
  def apply(to: Mailbox): To = To(List(to))
}

sealed trait Mail extends Product with Serializable

final case class AsciiMail(
    from: From,
    to: To,
    cc: Option[Cc],
    bcc:Option[Bcc],
    subject: Option[Subject],
    body: Option[Ascii]
) extends Mail

final case class MimeMail(
                           from: From,
                           to: To,
                           cc: Option[Cc],
                           bcc:Option[Bcc],
                           subject: Option[Subject],
                           body: Option[Body],
                           attachments:List[Attachment],
                           boundary:Boundary
                         ) extends Mail {

  def addAttachment(attachment: Attachment):MimeMail =
    copy(attachments = attachments :+ attachment)

  def addCC(mb:Mailbox):MimeMail = cc match {
    case Some(value) => copy(cc = Some(Cc(value.boxes:+mb)))
    case None => copy(cc = Some(Cc(List(mb))))
  }
}

object Mail {

  def ascii(from: From, to: To, subject: Subject, body: Ascii):AsciiMail =
    AsciiMail(from, to, None, None, Some(subject), Some(body))

  def mime(from: From, to: To, subject: Subject, body: Body):MimeMail = {
    val boundary = Boundary.genFrom(from.box.address)
    MimeMail(from, to, None, None, Some(subject), Some(body), Nil, boundary)
  }

}


