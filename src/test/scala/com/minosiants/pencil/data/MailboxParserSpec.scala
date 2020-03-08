package com.minosiants.pencil.data

import com.minosiants.pencil.data.Error.InvalidMailBox
import org.specs2.execute.Result
import org.specs2.mutable.Specification

class MailboxParserSpec extends Specification {

  "MailboxParser" should {

    "parse correctly" in {

      val emails = List(
        "name.ru@somedomain.com",
        ".dha134@dom.nab.com",
        "dha134@dom.nab22.com",
        "sdfe33d@saf-sf.com"
      )

      Result.foreach(emails) { e =>
        val result       = MailboxParser.parse(e)
        val Array(lp, d) = e.split("@")
        result must beRight(Mailbox(lp, d))
      }
    }

    "parse with error" in {

      val emails = List(
        "name..ru@somedomain.com",
        "dha134@-dom.nab22.com",
        "sdfe33d@saf-sf.com-",
        "sdfe33d@-saf-sf-.com",
        "sd>fe33d@-saf-sf-.com",
        "sdfe33d@",
        "sdfe33d"
      )

      Result.foreach(emails) { e =>
        val result = MailboxParser.parse(e)
        result match {
          case Left(InvalidMailBox(_)) =>
            success
          case _ =>
            failure(s"$e has not been parsed correctly ")
        }
      }
    }
  }
}
