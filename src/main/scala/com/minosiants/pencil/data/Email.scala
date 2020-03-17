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
package data

import Body._
import cats.data.NonEmptyList

sealed abstract class Email extends Product with Serializable {
  def from: From
  def to: To
  def cc: Option[Cc]
  def bcc: Option[Bcc]
  def subject: Option[Subject]
  def recipients: NonEmptyList[Mailbox] = (cc, bcc) match {
    case (Some(cc), Some(bcc)) => to.boxes ::: cc.boxes ::: bcc.boxes
    case (None, Some(bcc))     => to.boxes ::: bcc.boxes
    case (Some(cc), None)      => to.boxes ::: cc.boxes
    case (None, None)          => to.boxes
  }
}

object Email {

  final case class TextEmail(
      from: From,
      to: To,
      cc: Option[Cc],
      bcc: Option[Bcc],
      subject: Option[Subject],
      body: Option[Ascii]
  ) extends Email
      with AsciiMailOps

  final case class MimeEmail(
      from: From,
      to: To,
      cc: Option[Cc],
      bcc: Option[Bcc],
      subject: Option[Subject],
      body: Option[Body],
      attachments: List[Attachment],
      boundary: Boundary
  ) extends Email
      with MimeEmailOps

  def text(from: From, to: To, subject: Subject, body: Ascii): TextEmail =
    TextEmail(from, to, None, None, Some(subject), Some(body))

  def mime(from: From, to: To, subject: Subject, body: Body): MimeEmail = {
    val boundary = Boundary.genFrom(from.box.address)
    MimeEmail(from, to, None, None, Some(subject), Some(body), Nil, boundary)
  }

}

import Email._

trait AsciiMailOps {

  self: TextEmail =>
  def setCc(cc: Cc): TextEmail = copy(cc = Some(cc))

  def addCc(mb: Mailbox*): TextEmail = this.cc match {
    case Some(value) => copy(cc = Some(value + Cc(mb: _*)))
    case None        => copy(cc = Some(Cc(mb: _*)))
  }
  def addCc(cc: Cc): TextEmail = addCc(cc.boxes.toList: _*)
  def +(cc: Cc): TextEmail     = addCc(cc)

  def setBcc(bcc: Bcc): TextEmail = copy(bcc = Some(bcc))
  def addBcc(mb: Mailbox*): TextEmail = this.bcc match {
    case Some(value) => copy(bcc = Some(value + Bcc(mb: _*)))
    case None        => copy(bcc = Some(Bcc(mb: _*)))
  }
  def addBcc(bcc: Bcc): TextEmail = addBcc(bcc.boxes.toList: _*)
  def +(bcc: Bcc): TextEmail      = addBcc(bcc)

  def setBody(body: Ascii): TextEmail         = copy(body = Some(body))
  def setSubject(subject: Subject): TextEmail = copy(subject = Some(subject))
  def setFrom(from: From): TextEmail          = copy(from = from)
  def setTo(to: To): TextEmail                = copy(to = to)
  def addTo(to: Mailbox*): TextEmail =
    copy(to = To(this.to.boxes ++ to.toList))
  def addTo(to: To): TextEmail = addTo(to.boxes.toList: _*)
  def +(to: To): TextEmail     = addTo(to)
}

trait MimeEmailOps {
  self: MimeEmail =>

  def addAttachment(attachment: Attachment): MimeEmail =
    copy(attachments = attachments :+ attachment)
  def +(a: Attachment): MimeEmail = self.addAttachment(a)

  def setCc(cc: Cc): MimeEmail = copy(cc = Some(cc))

  def addCc(mb: Mailbox*): MimeEmail = this.cc match {
    case Some(value) => copy(cc = Some(value + Cc(mb: _*)))
    case None        => copy(cc = Some(Cc(mb: _*)))
  }
  def addCc(cc: Cc): MimeEmail = addCc(cc.boxes.toList: _*)
  def +(cc: Cc): MimeEmail     = addCc(cc)

  def setBcc(bcc: Bcc): MimeEmail = copy(bcc = Some(bcc))

  def addBcc(mb: Mailbox*): MimeEmail = this.bcc match {
    case Some(value) => copy(bcc = Some(value + Bcc(mb: _*)))
    case None        => copy(bcc = Some(Bcc(mb: _*)))
  }
  def addBcc(bcc: Bcc): MimeEmail = addBcc(bcc.boxes.toList: _*)
  def +(bcc: Bcc): MimeEmail      = addBcc(bcc)

  def setBody(body: Body): MimeEmail = copy(body = Some(body))

  def setSubject(subject: Subject): MimeEmail = copy(subject = Some(subject))
  def setFrom(from: From): MimeEmail          = copy(from = from)
  def setTo(to: To): MimeEmail                = copy(to = to)
  def addTo(to: Mailbox*): MimeEmail          = copy(to = To(this.to.boxes ++ to.toList))
  def addTo(to: To): MimeEmail                = addTo(to.boxes.toList: _*)
  def +(to: To): MimeEmail                    = addTo(to)
  def isMultipart: Boolean                    = attachments.nonEmpty

}
