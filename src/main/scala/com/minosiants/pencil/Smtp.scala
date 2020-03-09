package com.minosiants.pencil

import java.nio.file.spi.FileTypeDetector

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

  def endEmail(): Smtp[Replies] = text(Command.endEmail) >> read

  /*def body(): Smtp[Option[Replies]] = Smtp { req =>
    req.email.body match {
      case Some(Body(b)) => (text(s"$b") >> endEmail()).run(req).map(Some(_))
      case None          => IO(None)
    }
  }
   */
  def subjectHeader(): Smtp[Option[Unit]] = Smtp { req =>
    req.email.subject match {
      case Some(Subject(s)) =>
        text(s"Subject: $s ${Command.end}").run(req).map(Some(_))
      case None => IO(None)
    }
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
  def mimeHeader(): Smtp[Unit] = text(s"MIME-Version: 1.0 ${Command.end}")

  def contentTypeHeader(
      ct: ContentType,
      props: Map[String, String] = Map.empty
  ): Smtp[Unit] = {
    val values = (ct.show :: props.foldRight(List.empty[String]) {
      (item, acc) =>
        acc :+ s"${item._1}=${item._2}"
    }).mkString(";")
    //
    text(s"Content Type: $values")
  }

  def boundary(isFinal: Boolean = false): Smtp[Option[Unit]] = Smtp { req =>
    req.email match {
      case AsciiEmail(_, _, _, _, _, _) => IO(None)
      case MimeEmail(_, _, _, _, _, _, _, Boundary(b)) =>
        val end = if (isFinal) "---" else ""
        text(s"---$b$end").run(req).map(Some(_))
    }
  }

  def attachments(): Smtp[Option[Unit]] = Smtp { req =>
    req.email match {
      case AsciiEmail(_, _, _, _, _, _) => IO(None)
      case MimeEmail(_, _, _, _, _, _, attach, _) =>
        attach.map { a =>
          /*val r = Files.resource(a.file).use{bv =>
           IO("")
           FileTypeDetector
         }*/

          ???
          ///findContentType(a)
        }
    }
    ???
  }

}
