package com.minosiants.pencil

import cats.data.Kleisli
import cats.effect.IO
import protocol._
import data._
import cats.syntax.flatMap._
import cats.syntax.show._
import cats.syntax.traverse._
import cats.instances.list._

import scala.Function._
import com.minosiants.pencil.data.{ Email, Mailbox }
import Header._
import ContentType._
import com.minosiants.pencil.data.Body.{ Ascii, Html, Utf8 }
import scodec.bits.BitVector

final case class Request(email: Email, socket: SmtpSocket) {}

object Smtp {

  def apply[A](run: Request => IO[A]): Smtp[A] =
    Kleisli(req => run(req))

  def write(run: Email => Command): Smtp[Unit] = Smtp { req =>
    req.socket.write(run(req.email))
  }

  def read(): Smtp[Replies] = Smtp(_.socket.read()).flatMapF(processErrors)

  def processErrors(replies: Replies): IO[Replies] =
    if (replies.success) IO(replies) else Error.smtpError(replies.show)

  def command1(run: Email => Command): Smtp[Replies] = write(run) >> read()

  def command(c: Command): Smtp[Replies] = command1(const(c))

  def init(): Smtp[Replies] = read

  def ehlo(): Smtp[Replies] = command(Ehlo("pencil"))

  def mail(): Smtp[Replies] = command1(m => Mail(m.from.box))

  def rcpt(): Smtp[List[Replies]] = Smtp { req =>
    val rcptCommand = (m: Mailbox) => command(Rcpt(m)).run(req)
    val ccValue     = req.email.cc.map(_.boxes).getOrElse(List.empty[Mailbox])
    val bccValue    = req.email.bcc.map(_.boxes).getOrElse(List.empty[Mailbox])
    for {
      to  <- req.email.to.boxes.traverse(rcptCommand)
      cc  <- ccValue.traverse(rcptCommand)
      bcc <- bccValue.traverse(rcptCommand)
    } yield to ++ cc ++ bcc

  }

  def data(): Smtp[Replies] = command(Data)

  def rset(): Smtp[Replies] = command(Rset)

  def vrfy(str: String): Smtp[Replies] = command(Vrfy(str))

  def noop(): Smtp[Replies] = command(Noop)

  def quit(): Smtp[Replies] = command(Quit)

  def text(txt: String): Smtp[Unit] = write(const(Text(txt)))

  def endEmail(): Smtp[Replies] = Smtp { req =>
    val p = req.email match {
      case AsciiEmail(_, _, _, _, _, _) => text(Command.endEmail) >> read
      case _ =>
        for {
          _ <- boundary(true)
          _ <- text(Command.endEmail)
          r <- read
        } yield r
    }
    p.run(req)

  }

  def asciiBody(): Smtp[Replies] = Smtp { req =>
    req.email match {
      case AsciiEmail(_, _, _, _, _, Some(Ascii(body))) =>
        (text(s"$body") >> endEmail()).run(req)
      case _ => Error.smtpError("Body is not ascii")
    }
  }

  def subjectHeader(): Smtp[Option[Unit]] = Smtp { req =>
    req.email.subject match {
      case Some(Subject(s)) =>
        text(s"Subject: $s ${Command.end}").run(req).map(Some(_))
      case None => IO(None)
    }
  }

  def fromHeader(): Smtp[Unit] = Smtp { req =>
    text(s"From: ${req.email.from.show} ${Command.end}").run(req)
  }
  def toHeader(): Smtp[Unit] = Smtp { req =>
    text(s"To: ${req.email.to.show} ${Command.end}").run(req)
  }

  def ccHeader(): Smtp[Option[Unit]] = Smtp { req =>
    req.email.cc match {
      case Some(v) =>
        text(s"Cc: ${v.show} ${Command.end}").run(req).map(Some(_))
      case None => IO(None)
    }
  }

  def bccHeader(): Smtp[Option[Unit]] = Smtp { req =>
    req.email.bcc match {
      case Some(v) =>
        text(s"Bcc: ${v.show} ${Command.end}").run(req).map(Some(_))
      case None => IO(None)
    }
  }
  def mainHeaders(): Smtp[Unit] =
    for {
      _ <- fromHeader()
      _ <- toHeader()
      _ <- ccHeader()
      _ <- bccHeader()
      _ <- subjectHeader()
    } yield ()

  def mimeHeader(): Smtp[Unit] = {
    text(s"${headerShow.show(`MIME-Version`())} ${Command.end}")
  }

  def contentTypeHeader(
      ct: `Content-Type`
  ): Smtp[Unit] = text(s"${headerShow.show(ct)}")

  def contentTransferEncoding(encoding: Encoding) =
    text(
      s"${headerShow.show(`Content-Transfer-Encoding`(encoding))} ${Command.end}"
    )

  def boundary(isFinal: Boolean = false): Smtp[Option[Unit]] = Smtp { req =>
    req.email match {
      case AsciiEmail(_, _, _, _, _, _) => IO(None)
      case MimeEmail(_, _, _, _, _, _, _, Boundary(b)) =>
        val end = if (isFinal) "---" else ""
        text(s"---$b$end").run(req).map(Some(_))
    }
  }

  def mimePart(body: String, mech: Encoding, ct: `Content-Type`): Smtp[Unit] =
    for {
      _ <- boundary()
      _ <- contentTypeHeader(ct)
      _ <- contentTransferEncoding(mech)
      _ <- text(s"$body ${Command.end}")
    } yield ()

  def mimeBody(): Smtp[Option[Unit]] = Smtp { req =>
    def sender(ct: `Content-Type`, body: String) =
      (for {
        _ <- boundary()
        _ <- contentTypeHeader(ct)
        _ <- text(s"$body ${Command.end}")
      } yield ()).run(req).map(Some(_))

    req.email match {
      case MimeEmail(_, _, _, _, _, Some(Ascii(body)), _, _) =>
        sender(`Content-Type`(`text/plain`, Map("charset" -> "US-ASCII")), body)

      case MimeEmail(_, _, _, _, _, Some(Html(body)), _, _) =>
        sender(`Content-Type`(`text/html`, Map("charset" -> "UTF-8")), body)

      case MimeEmail(_, _, _, _, _, Some(Utf8(body)), _, boundary) =>
        sender(`Content-Type`(`text/plain`, Map("charset" -> "UTF-8")), body)

      case _ => IO(None)
    }

  }

  def attachments(): Smtp[Option[Unit]] = Smtp { req =>
    req.email match {
      case AsciiEmail(_, _, _, _, _, _) => IO(None)
      case MimeEmail(_, _, _, _, _, _, attach, _) =>
        val result = attach.map { a =>
          val res = Files.resource(a.file)
          for {
            encoded <- res.use(v => IO(BitVector.fromInputStream(v).toBase64))
            ct      <- res.use(ContentTypeFinder.findType)
            _ <- mimePart(encoded, Encoding.`base64`, `Content-Type`(ct))
              .run(req)
          } yield ()
        }
        result.sequence.map(_.headOption)
    }
  }

}
