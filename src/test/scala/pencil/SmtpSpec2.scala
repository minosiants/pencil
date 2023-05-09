package pencil

import cats.effect.IO
import pencil.syntax.*
import pencil.protocol.Code
import cats.syntax.flatMap.*
import pencil.data.{Body, Email}
import SmtpSpec2.mimeEmail
class SmtpSpec2 extends MailServerSpec {

  sequential

  "smtp command be" should {
    "ehlo" in {
      val r = Smtp.ehlo[IO]().runCommand
      r.replies.head.code === Code.`250`
    }
    "mail" in {
      val r = Smtp.mail[IO]().runCommand
      r.replies.head.code === Code.`250`
    }
    "rcpt" in {
      val r = (Smtp.mail[IO]() >> Smtp.rcpt[IO]()).runCommand
      r.head.replies.head.code === Code.`250`
    }
    "vrfy" in {
      val r = Smtp.vrfy[IO]("hello").runCommand
      r.replies.head.code === Code.`252`
    }
    "noop" in {
      val r = Smtp.noop[IO]().runCommand
      r.replies.head.code === Code.`250`
    }
    "quit" in {
      val r = Smtp.quit[IO]().runCommand
      r.replies.head.code === Code.`221`
    }
  }
}

object SmtpSpec2 extends LiteralsSyntax:
  given mimeEmail:Email =  Email.mime(
    from"user1@mydomain.tld",
    to"pencil@mail.pencil.com",
    subject"привет",
    Body.Utf8("hi there")
    //  List(attachment"files/jpeg-sample.jpg")
  )