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

import Body.*
import cats.data.NonEmptyList
import cats.syntax.semigroup.*
import cats.instances.option.*
import cats.syntax.option.*

import scala.annotation.targetName

enum EmailType:
  case Text
  case Mime(boundary: Boundary, attachments: List[Attachment] = Nil)

  def +(a: Attachment): EmailType = this match {
    case Text                        => this
    case Mime(boundary, attachments) => Mime(boundary, a :: attachments)
  }

  def fold[A](text: => A, mime: Mime => A): A =
    this match
      case Text           => text
      case m @ Mime(_, _) => mime(m)

  def mime: Option[Mime]      = fold(None, _.some)
  def text: Option[Text.type] = fold(Some(Text), _ => None)
  def isMime: Boolean         = fold(false, _ => true)
  def isText: Boolean         = fold(true, _ => false)

final case class Email(
    from: From,
    to: To,
    cc: Option[Cc],
    bcc: Option[Bcc],
    subject: Option[Subject],
    body: Option[Body],
    emailType: EmailType
):
  def recipients: NonEmptyList[Mailbox] = (cc, bcc) match {
    case (Some(cc), Some(bcc)) =>
      to.mailboxes ::: cc.mailboxes ::: bcc.mailboxes
    case (None, Some(bcc)) => to.mailboxes ::: bcc.mailboxes
    case (Some(cc), None)  => to.mailboxes ::: cc.mailboxes
    case (None, None)      => to.mailboxes
  }

  /** Replace `cc` with a new value.
    */
  def setCc(cc: Cc): Email = copy(cc = Some(cc))

  /** Add [[Mailbox]] to `cc` .
    */
  def addCc(mb: Mailbox*): Email = copy(cc = cc |+| Some(Cc(mb*)))

  /** Combine values from both `cc`.
    */
  def addCc(cc: Cc): Email = addCc(cc.toList*)

  /** Combine values from both `cc`.
    */
  def +(cc: Cc): Email = addCc(cc)

  /** Replace `bcc` with a new value.
    */
  def setBcc(bcc: Bcc): Email = copy(bcc = Some(bcc))

  /** Add [[Mailbox]] to `bcc`.
    */
  def addBcc(mb: Mailbox*): Email = copy(bcc = bcc |+| Some(Bcc(mb*)))

  /** Combine to `bcc`.
    */
  def addBcc(bcc: Bcc): Email = addBcc(bcc.toList*)

  /** Combine to `bcc`.
    */
  @targetName("+bcc")
  def +(bcc: Bcc): Email = addBcc(bcc)

  /** Set `body` value. Replace existing one with a new one.
    */
  def setBody(body: Body): Email = copy(body = Some(body))

  /** Set `subject`. Replace existing one with a new one.
    */
  def setSubject(subject: Subject): Email = copy(subject = Some(subject))

  /** Set `from`. Replace existing one with a new one.
    */
  def setFrom(from: From): Email = copy(from = from)

  /** Set `to`. Replace existing one with a new one.
    */
  def setTo(to: To): Email = copy(to = to)

  /** Add [[Mailbox]] to `to` value.
    */
  def addTo(to: Mailbox*): Email =
    copy(to = this.to + To(to*))

  /** Combine `to` values.
    */
  def addTo(to: To): Email = addTo(to.toList: _*)

  /** Combine `to` values.
    */
  @targetName("+to")
  def +(to: To): Email = addTo(to)

  /** Check if it is multipart.
    *
    * @return
    *   \- `true` it is a mutlipart email.
    */
  def isMultipart: Boolean = emailType match
    case EmailType.Text       => false
    case EmailType.Mime(_, a) => a.nonEmpty

  def addAttachment(attachment: Attachment): Email = this + attachment
  def +(attachment: Attachment): Email =
    this.copy(emailType = emailType + attachment)
  def attachments: Option[List[Attachment]] = emailType.mime.map(_.attachments)
  def boundary: Option[Boundary]            = emailType.mime.map(_.boundary)
  def isMime: Boolean                       = emailType.isMime
  def isText: Boolean                       = emailType.isText

object Email:
  /** [[TextEmail]] constructor
    */
  def text(from: From, to: To, subject: Subject, body: Ascii): Email =
    Email(from, to, None, None, Some(subject), Some(body), EmailType.Text)

  /** [[MimeEmail]] constructor
    */
  def mime(
      from: From,
      to: To,
      subject: Subject,
      body: Body,
      attachments: List[Attachment] = Nil
  ): Email = {
    val boundary = Boundary.genFrom(from.mailbox.address)
    Email(
      from,
      to,
      None,
      None,
      Some(subject),
      Some(body),
      EmailType.Mime(boundary, attachments)
    )
  }
