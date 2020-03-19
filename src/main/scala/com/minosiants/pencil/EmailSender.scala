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

import com.minosiants.pencil.data.Email
import com.minosiants.pencil.data.Email.{MimeEmail, TextEmail}
import com.minosiants.pencil.protocol.Replies

trait EmailSender[A <: Email] {
  def send(): Smtp[Replies]
}

object EmailSender {

  implicit lazy val textEmailSender: EmailSender[TextEmail] =
    new EmailSender[TextEmail] {
      override def send(): Smtp[Replies] =
        for {
          _ <- Smtp.mail()
          _ <- Smtp.rcpt()
          _ <- Smtp.data()
          _ <- Smtp.mainHeaders()
          r <- Smtp.asciiBody()
          _ <- Smtp.quit()
        } yield r
    }

  implicit lazy val mimeEmailSender: EmailSender[MimeEmail] =
    new EmailSender[MimeEmail] {
      override def send(): Smtp[Replies] =
        for {
          _ <- Smtp.mail()
          _ <- Smtp.rcpt()
          _ <- Smtp.data()
          _ <- Smtp.mimeHeader()
          _ <- Smtp.mainHeaders()
          _ <- Smtp.multipart()
          _ <- Smtp.mimeBody()
          _ <- Smtp.attachments()
          r <- Smtp.endEmail()
          _ <- Smtp.quit()
        } yield r
    }

}
