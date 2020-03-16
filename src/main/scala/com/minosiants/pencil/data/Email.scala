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

sealed abstract class Email extends Product with Serializable {
  def from: From
  def to: To
  def cc: Option[Cc]
  def bcc: Option[Bcc]
  def subject: Option[Subject]
}

object Email {

  final case class AsciiEmail(
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

  def ascii(from: From, to: To, subject: Subject, body: Ascii): AsciiEmail =
    AsciiEmail(from, to, None, None, Some(subject), Some(body))

  def mime(from: From, to: To, subject: Subject, body: Body): MimeEmail = {
    val boundary = Boundary.genFrom(from.box.address)
    MimeEmail(from, to, None, None, Some(subject), Some(body), Nil, boundary)
  }

}

import Email._

trait AsciiMailOps {

  self: AsciiEmail =>
  def setCc(cc: Cc): AsciiEmail = copy(cc = Some(cc))

  def addCc(mb: Mailbox*): AsciiEmail = this.cc match {
    case Some(value) => copy(cc = Some(Cc(value.boxes ++ mb)))
    case None        => copy(cc = Some(Cc(mb.toList)))
  }
  def addCc(cc: Cc): AsciiEmail = addCc(cc.boxes: _*)
  def +(cc: Cc): AsciiEmail     = addCc(cc)

  def setBcc(bcc: Bcc): AsciiEmail = copy(bcc = Some(bcc))
  def addBcc(mb: Mailbox*): AsciiEmail = this.bcc match {
    case Some(value) => copy(bcc = Some(Bcc(value.boxes ++ mb)))
    case None        => copy(bcc = Some(Bcc(mb.toList)))
  }
  def addBcc(bcc: Bcc): AsciiEmail = addBcc(bcc.boxes: _*)
  def +(bcc: Bcc): AsciiEmail      = addBcc(bcc)

  def setBody(body: Ascii): AsciiEmail         = copy(body = Some(body))
  def setSubject(subject: Subject): AsciiEmail = copy(subject = Some(subject))
  def setFrom(from: From): AsciiEmail          = copy(from = from)
  def setTo(to: To): AsciiEmail                = copy(to = to)
  def addTo(to: Mailbox*): AsciiEmail =
    copy(to = To(this.to.boxes ++ to.toList))
  def addTo(to: To): AsciiEmail = addTo(to.boxes: _*)
  def +(to: To): AsciiEmail     = addTo(to)
}

trait MimeEmailOps {
  self: MimeEmail =>

  def addAttachment(attachment: Attachment): MimeEmail =
    copy(attachments = attachments :+ attachment)
  def +(a: Attachment): MimeEmail = self.addAttachment(a)

  def setCc(cc: Cc): MimeEmail = copy(cc = Some(cc))

  def addCc(mb: Mailbox*): MimeEmail = this.cc match {
    case Some(value) => copy(cc = Some(Cc(value.boxes ++ mb)))
    case None        => copy(cc = Some(Cc(mb.toList)))
  }
  def addCc(cc: Cc): MimeEmail = addCc(cc.boxes: _*)
  def +(cc: Cc): MimeEmail     = addCc(cc)

  def setBcc(bcc: Bcc): MimeEmail = copy(bcc = Some(bcc))

  def addBcc(mb: Mailbox*): MimeEmail = this.bcc match {
    case Some(value) => copy(bcc = Some(Bcc(value.boxes ++ mb)))
    case None        => copy(bcc = Some(Bcc(mb.toList)))
  }
  def addBcc(bcc: Bcc): MimeEmail = addBcc(bcc.boxes: _*)
  def +(bcc: Bcc): MimeEmail      = addBcc(bcc)

  def setBody(body: Body): MimeEmail = copy(body = Some(body))

  def setSubject(subject: Subject): MimeEmail = copy(subject = Some(subject))
  def setFrom(from: From): MimeEmail          = copy(from = from)
  def setTo(to: To): MimeEmail                = copy(to = to)
  def addTo(to: Mailbox*): MimeEmail          = copy(to = To(this.to.boxes ++ to.toList))
  def addTo(to: To): MimeEmail                = addTo(to.boxes: _*)
  def +(to: To): MimeEmail                    = addTo(to)
  def isMultipart: Boolean                    = attachments.nonEmpty

}
