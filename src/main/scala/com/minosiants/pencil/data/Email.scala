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

sealed trait Email extends Product with Serializable {
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

trait MimeEmailOps {
  self: MimeEmail =>

  def addAttachment(attachment: Attachment): MimeEmail =
    copy(attachments = attachments :+ attachment)
  def +(a: Attachment): MimeEmail = self.addAttachment(a)

  def addCC(mb: Mailbox): MimeEmail = this.cc match {
    case Some(value) => copy(cc = Some(Cc(value.boxes :+ mb)))
    case None        => copy(cc = Some(Cc(List(mb))))
  }
  //def + (cc:Cc):MimeEmail = cc.boxes.map(this.addCC(_))

  def addBcc(mb: Mailbox): MimeEmail = this.bcc match {
    case Some(value) => copy(bcc = Some(Bcc(value.boxes :+ mb)))
    case None        => copy(bcc = Some(Bcc(List(mb))))
  }

  def setBody(body: Body): MimeEmail = copy(body = Some(body))

  def isMultipart: Boolean = attachments.nonEmpty

}
