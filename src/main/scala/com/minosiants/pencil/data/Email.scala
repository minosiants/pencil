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
import cats.syntax.semigroup._
import cats.instances.option._

/** Abstract class represents email
  */
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

  /** Represents text (ascii) email
    *
    * @param from
    *   \- [[From]] contains sender [[Mailbox]]
    * @param to
    *   \- [[To]] contains list of recipients
    * @param cc
    *   \- [[Cc]] optional param. Contains cc recipients
    * @param bcc
    *   \- [[Bcc]] optional param. Contains bcc recipients
    * @param subject
    *   \- [[Subject]] optional param. Contains email subject
    * @param body
    *   \- [[Ascii]] optional param. Contains email body
    */
  final case class TextEmail(
      from: From,
      to: To,
      cc: Option[Cc],
      bcc: Option[Bcc],
      subject: Option[Subject],
      body: Option[Ascii]
  ) extends Email
      with TextEmailOps

  /** Represents mime email
    *
    * @param from
    *   \- [[From]] contains sender [[Mailbox]]
    * @param to
    *   \- [[To]] contains list of recipients
    * @param cc
    *   \- [[Cc]] optional param. Contains cc recipients
    * @param bcc
    *   \- [[Bcc]] optional param. Contains bcc recipients
    * @param subject
    *   \- [[Subject]] optional param. Contains email subject
    * @param body
    *   \- [[Body]] optional param. Contains email body
    * @param attachments
    *   \- [[List[Attachment]]] - list of attachments
    * @param boundary
    *   \- [[Boundary]] - used for multi part separation
    */
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

  /** [[TextEmail]] constructor
    */
  def text(from: From, to: To, subject: Subject, body: Ascii): TextEmail =
    TextEmail(from, to, None, None, Some(subject), Some(body))

  /** [[MimeEmail]] constructor
    */
  def mime(from: From, to: To, subject: Subject, body: Body): MimeEmail = {
    val boundary = Boundary.genFrom(from.box.address)
    MimeEmail(from, to, None, None, Some(subject), Some(body), Nil, boundary)
  }

}

import Email._

trait TextEmailOps {
  self: TextEmail =>

  /** Replace `cc` with a new value.
    */
  def setCc(cc: Cc): TextEmail = copy(cc = Some(cc))

  /** Add [[Mailbox]] to `cc` .
    */
  def addCc(mb: Mailbox*): TextEmail = copy(cc = cc |+| Some(Cc(mb: _*)))

  /** Combine values from both `cc`.
    */
  def addCc(cc: Cc): TextEmail = addCc(cc.boxes.toList: _*)

  /** Combine values from both `cc`.
    */
  def +(cc: Cc): TextEmail = addCc(cc)

  /** Replace `bcc` with a new value.
    */
  def setBcc(bcc: Bcc): TextEmail = copy(bcc = Some(bcc))

  /** Add [[Mailbox]] to `bcc`.
    */
  def addBcc(mb: Mailbox*): TextEmail = copy(bcc = bcc |+| Some(Bcc(mb: _*)))

  /** Combine to `bcc`.
    */
  def addBcc(bcc: Bcc): TextEmail = addBcc(bcc.boxes.toList: _*)

  /** Combine to `bcc`.
    */
  def +(bcc: Bcc): TextEmail = addBcc(bcc)

  /** Set `body` value. Replace existing one with a new one.
    */
  def setBody(body: Ascii): TextEmail = copy(body = Some(body))

  /** Set `subject`. Replace existing one with a new one.
    */
  def setSubject(subject: Subject): TextEmail = copy(subject = Some(subject))

  /** Set `from`. Replace existing one with a new one.
    */
  def setFrom(from: From): TextEmail = copy(from = from)

  /** Set `to`. Replace existing one with a new one.
    */
  def setTo(to: To): TextEmail = copy(to = to)

  /** Add [[Mailbox]] to `to` value.
    */
  def addTo(to: Mailbox*): TextEmail =
    copy(to = To(this.to.boxes ++ to.toList))

  /** Combine `to` values.
    */
  def addTo(to: To): TextEmail = addTo(to.boxes.toList: _*)

  /** Combine `to` values.
    */
  def +(to: To): TextEmail = addTo(to)
}

trait MimeEmailOps {
  self: MimeEmail =>

  /** Add [[Attachment]].
    */
  def addAttachment(attachment: Attachment): MimeEmail =
    copy(attachments = attachments :+ attachment)

  /** Add [[Attachment]].
    */
  def +(a: Attachment): MimeEmail = self.addAttachment(a)

  /** Set cc. Replace existing one with a new one.
    */
  def setCc(cc: Cc): MimeEmail = copy(cc = Some(cc))

  /** Add [[Mailbox]] to `cc`.
    */
  def addCc(mb: Mailbox*): MimeEmail = copy(cc = cc |+| Some(Cc(mb: _*)))

  /** Combine `cc` values.
    */
  def addCc(cc: Cc): MimeEmail = addCc(cc.boxes.toList: _*)

  /** Combine `cc` values.
    */
  def +(cc: Cc): MimeEmail = addCc(cc)

  /** Set `bcc`. Replace an existing one with a new one.
    */
  def setBcc(bcc: Bcc): MimeEmail = copy(bcc = Some(bcc))

  /** Add [[Mailbox]] to `bcc` value.
    */
  def addBcc(mb: Mailbox*): MimeEmail = copy(bcc = bcc |+| Some(Bcc(mb: _*)))

  /** Combine `bcc` values
    */
  def addBcc(bcc: Bcc): MimeEmail = addBcc(bcc.boxes.toList: _*)

  /** Combine `bcc` values
    */
  def +(bcc: Bcc): MimeEmail = addBcc(bcc)

  /** Set `body`. Replace an existing value with a new one.
    */
  def setBody(body: Body): MimeEmail = copy(body = Some(body))

  /** Set `subject`. Replace an existing value with a new one.
    */
  def setSubject(subject: Subject): MimeEmail = copy(subject = Some(subject))

  /** Set `from`. Replace an existing value with a new one.
    */
  def setFrom(from: From): MimeEmail = copy(from = from)

  /** Set `to`. Replace an existing value with a new one.
    */
  def setTo(to: To): MimeEmail = copy(to = to)

  /** Add [[Mailbox]] to `to`.
    */
  def addTo(to: Mailbox*): MimeEmail = copy(to = this.to |+| To(to: _*))

  /** Combine `to` values.
    */
  def addTo(to: To): MimeEmail = addTo(to.boxes.toList: _*)

  /** Combine `to` values.
    */
  def +(to: To): MimeEmail = addTo(to)

  /** Check if it is multipart.
    *
    * @return
    *   \- `true` it is a mutlipart email.
    */
  def isMultipart: Boolean = attachments.nonEmpty

}
