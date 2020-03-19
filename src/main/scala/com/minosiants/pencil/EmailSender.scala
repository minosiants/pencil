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
