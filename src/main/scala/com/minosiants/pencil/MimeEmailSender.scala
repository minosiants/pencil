package com.minosiants.pencil

import com.minosiants.pencil.data.Credentials
import com.minosiants.pencil.protocol.Replies
import data.Email.MimeEmail

final case class MimeEmailSender()
    extends EmailSender[MimeEmail]
    with Product
    with Serializable {

  override def sendEmail(credentials: Option[Credentials]): Smtp[Replies] =
    for {
      _ <- credentials.fold(Smtp.pure(()))(Smtp.login)
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
