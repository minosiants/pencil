package com.minosiants.pencil

import cats.effect.{ Blocker, IO }
import cats.syntax.show._
import com.minosiants.pencil.SmtpSpec.body
import com.minosiants.pencil.data.Body.{ Ascii, Html, Utf8 }
import com.minosiants.pencil.data.Email._
import com.minosiants.pencil.data._
import com.minosiants.pencil.protocol.ContentType.`application/pdf`
import com.minosiants.pencil.protocol.Encoding.`base64`
import com.minosiants.pencil.protocol.Header.`Content-Type`
import com.minosiants.pencil.protocol._
import scodec.codecs

class SmtpSpec extends SmtpBaseSpec {

  sequential

  "Smtp" should {

    "get response on EHLO" in {
      val host   = Host.local()
      val result = testCommand(Smtp.ehlo(), SmtpSpec.mime, codecs.ascii)
      result.map(_._1) must beRight(DataSamples.ehloReplies)
      result.map(_._2) must beRight(List(s"EHLO ${host.name}${Command.end}"))
    }

    "get response on RCPT" in {
      val email  = SmtpSpec.mime
      val result = testCommand(Smtp.rcpt(), email, codecs.ascii)
      val rcpts = email.recipients
        .map(box => s"RCPT TO: ${box.show}${Command.end}")
        .toList

      result.map(_._1) must beRight(List.fill(3)(DataSamples.`250 OK`))
      result.map(_._2) must beRight(rcpts)
    }

    "get response on MAIL" in {
      val result = testCommand(Smtp.mail(), SmtpSpec.mime, codecs.ascii)
      val from   = SmtpSpec.mime.from.box
      result.map(_._1) must beRight(DataSamples.`250 OK`)
      result.map(_._2) must beRight(
        List(s"MAIL FROM: ${from.show}${Command.end}")
      )
    }

    "get response on DATA" in {
      val result = testCommand(Smtp.data(), SmtpSpec.mime, codecs.ascii)
      result.map(_._1) must beRight(DataSamples.`354 End data`)
      result.map(_._2) must beRight(List(s"DATA${Command.end}"))
    }

    "get response on QUIT" in {
      val result = testCommand(Smtp.quit(), SmtpSpec.mime, codecs.ascii)
      result.map(_._1) must beRight(DataSamples.`221 Buy`)
      result.map(_._2) must beRight(List(s"QUIT${Command.end}"))
    }

    "send text via Text command " in {
      val result = testCommand(
        Smtp.text(s"Hello${Command.end}"),
        SmtpSpec.mime,
        codecs.ascii
      )
      result.map(_._2) must beRight(List(s"Hello${Command.end}"))
    }

    "send endEmail" in {
      val email  = SmtpSpec.mime
      val result = testCommand(Smtp.endEmail(), email, codecs.ascii)

      result.map(_._1) must beRight(DataSamples.`250 OK`)
      result.map(_._2) must beRight(
        List(
          s"--${email.boundary.value}--${Command.end}",
          s"${Command.endEmail}"
        )
      )
    }

    "send asciiBody" in {
      val email  = SmtpSpec.text
      val result = testCommand(Smtp.asciiBody(), email, codecs.ascii)

      result.map(_._1) must beRight(DataSamples.`250 OK`)
      result.map(_._2) must beRight(
        List(
          s"${email.body.get.value}${Command.end}",
          s"${Command.endEmail}"
        )
      )
    }

    "send subjectHeader in ascii mail" in {
      val email  = SmtpSpec.text
      val result = testCommand(Smtp.subjectHeader(), email, codecs.ascii)
      result.map(_._2) must beRight(
        List(
          s"Subject: ${email.subject.get.value}${Command.end}"
        )
      )
    }
    "send subjectHeader in mime mail" in {
      val email  = SmtpSpec.mime
      val result = testCommand(Smtp.subjectHeader(), email, codecs.ascii)
      result.map(_._2) must beRight(
        List(
          s"Subject: =?utf-8?b?${email.subject.get.value.toBase64}?=${Command.end}"
        )
      )
    }

    "send fromHeader" in {
      val email  = SmtpSpec.text
      val result = testCommand(Smtp.fromHeader(), email, codecs.ascii)
      result.map(_._2) must beRight(
        List(
          s"From: ${email.from.show}${Command.end}"
        )
      )
    }
    "send toHeader" in {
      val email  = SmtpSpec.text
      val result = testCommand(Smtp.toHeader(), email, codecs.ascii)
      result.map(_._2) must beRight(
        List(
          s"To: ${email.to.show}${Command.end}"
        )
      )
    }

    "send ccHeader" in {
      val email  = SmtpSpec.mime
      val result = testCommand(Smtp.ccHeader(), email, codecs.ascii)
      result.map(_._2) must beRight(
        List(
          s"Cc: ${email.cc.get.show}${Command.end}"
        )
      )
    }
    "send bccHeader" in {
      val email  = SmtpSpec.mime
      val result = testCommand(Smtp.bccHeader(), email, codecs.ascii)
      result.map(_._2) must beRight(
        List(
          s"Bcc: ${email.bcc.get.show}${Command.end}"
        )
      )
    }
    "send mainHeaders" in {
      val email  = SmtpSpec.mime
      val result = testCommand(Smtp.mainHeaders(), email, codecs.ascii)
      result.map(_._2.size) must beRight(7)
      //TODO refactor to test all headers
      result.map(_._2) must beRight(
        List(
          s"Date: ${Smtp.dateFormatter.format(timestamp)}${Command.end}",
          s"From: ${email.from.show}${Command.end}",
          s"To: ${email.to.show}${Command.end}",
          s"Cc: ${email.cc.get.show}${Command.end}",
          s"Bcc: ${email.bcc.get.show}${Command.end}",
          s"Message-ID: <$uuid.${timestamp.getEpochSecond}@${host.name}>${Command.end}",
          s"Subject: =?utf-8?b?${email.subject.get.value.toBase64}?=${Command.end}"
        )
      )
    }
  }

  "send mimeHeader" in {
    val email  = SmtpSpec.mime
    val result = testCommand(Smtp.mimeHeader(), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"MIME-Version: 1.0${Command.end}"
      )
    )
  }

  "send contentTypeHeader" in {
    val email = SmtpSpec.mime
    val result = testCommand(
      Smtp.contentTypeHeader(
        `Content-Type`(
          `application/pdf`,
          Map("param1" -> "value1", "param2" -> "value2")
        )
      ),
      email,
      codecs.ascii
    )
    result.map(_._2) must beRight(
      List(
        s"Content-Type: application/pdf; param1=value1;param2=value2${Command.end}"
      )
    )
  }

  "send contentTransferEncoding" in {
    val email = SmtpSpec.mime
    val result =
      testCommand(Smtp.contentTransferEncoding(`base64`), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"Content-Transfer-Encoding: base64${Command.end}"
      )
    )
  }

  "send boundary" in {
    val email  = SmtpSpec.mime
    val result = testCommand(Smtp.boundary(), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"--${email.boundary.value}${Command.end}"
      )
    )
  }

  "send final boundary" in {
    val email  = SmtpSpec.mime
    val result = testCommand(Smtp.boundary(true), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"--${email.boundary.value}--${Command.end}"
      )
    )
  }

  "send multipart" in {
    val email  = SmtpSpec.mime
    val result = testCommand(Smtp.multipart(), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"Content-Type: multipart/mixed; boundary=${email.boundary.value}${Command.end}"
      )
    )
  }

  "send mime utf body" in {
    val email  = SmtpSpec.mime
    val result = testCommand(Smtp.mimeBody(), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"--${email.boundary.value}${Command.end}",
        s"Content-Type: text/plain; charset=UTF-8${Command.end}",
        s"Content-Transfer-Encoding: base64${Command.end}",
        s"${Command.end}"
      ) ++ body(email.body) {
        case Utf8(value) => value.toBase64
      }
    )
  }

  "send mime html body" in {
    val email  = SmtpSpec.mime.setBody(Html("<h1>hello</h1>"))
    val result = testCommand(Smtp.mimeBody(), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"--${email.boundary.value}${Command.end}",
        s"Content-Type: text/html; charset=UTF-8${Command.end}",
        s"Content-Transfer-Encoding: base64${Command.end}",
        s"${Command.end}"
      ) ++ body(email.body) {
        case Html(value) => value.toBase64
      }
    )
  }
  "send mime ascii body" in {
    val email  = SmtpSpec.mime.setBody(Ascii("hello"))
    val result = testCommand(Smtp.mimeBody(), email, codecs.ascii)
    result.map(_._2) must beRight(
      List(
        s"--${email.boundary.value}${Command.end}",
        s"Content-Type: text/plain; charset=US-ASCII${Command.end}",
        s"Content-Transfer-Encoding: 7bit${Command.end}",
        s"${Command.end}"
      ) ++ body(email.body) {
        case Ascii(value) => value
      }
    )
  }

  "send attachments" in {
    val email      = SmtpSpec.mime
    val attachment = email.attachments.head
    val result     = testCommand(Smtp.attachments(), email, codecs.ascii)

    val encodedFile = Blocker[IO]
      .use { blocker =>
        fs2.io.file
          .readAll[IO](attachment.file, blocker, 1024)
          .through(fs2.text.base64.encode)
          .compile
          .toList
          .map { list =>
            SmtpSpec.lines(list.mkString)
          }
      }
      .unsafeRunSync()
    val encodedAttachmentName =
      s"=?utf-8?b?${attachment.file.getFileName.toString.toBase64}?="

    result.map(_._2) must beRight(
      s"--${email.boundary.value}${Command.end}" ::
        s"Content-Type: image/png; name=${encodedAttachmentName}${Command.end}" ::
        s"Content-Transfer-Encoding: base64${Command.end}" ::
        s"${Command.end}" ::
        encodedFile
    )
  }

}

object SmtpSpec {

  def lines(str: String): List[String] =
    str.grouped(76).map(_ + Command.end).toList

  def body(
      value: Option[Body]
  )(pf: PartialFunction[Body, String]): List[String] = {
    value.map(pf).getOrElse("") match {
      case ""  => List.empty
      case str => lines(str)
    }
  }
  val text: TextEmail = {
    Email.text(
      From(mailbox"user1@mydomain.tld"),
      To(mailbox"user1@example.com"),
      subject"first email",
      Body.Ascii("hello")
    )
  }

  val mime: MimeEmail =
    Email
      .mime(
        From(mailbox"user1@mydomain.tld"),
        To(mailbox"user1@example.com"),
        subject"привет",
        Body.Utf8("hi there")
      )
      .addAttachment(
        Attachment.fromString[IO]("files/small.png").unsafeRunSync()
      )
      .addCc(mailbox"ccuser1@example.com")
      .addBcc(mailbox"bccuser1@example.com")

}
