package com.minosiants.pencil

import com.minosiants.pencil.data.Credentials
import com.minosiants.pencil.data.Email.TextEmail
import com.minosiants.pencil.protocol.Replies

final case class TextEmailSender() extends EmailSender[TextEmail] {

  override def sendEmail(credentials: Option[Credentials]): Smtp[Replies] =
    for {
      _ <- credentials.fold(Smtp.pure(()))(Smtp.login)
      _ <- Smtp.mail()
      _ <- Smtp.rcpt()
      _ <- Smtp.data()
      _ <- Smtp.mainHeaders()
      r <- Smtp.asciiBody()
      _ <- Smtp.quit()
    } yield r

}
