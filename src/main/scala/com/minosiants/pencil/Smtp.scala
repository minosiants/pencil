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

import cats.data.Kleisli
import cats.effect.IO
import protocol._
import data._
import cats.syntax.flatMap._
import cats.syntax.show._
import cats.syntax.traverse._
import cats.syntax.functor._
import cats.instances.list._

import scala.Function._
import com.minosiants.pencil.data.{ Email, Mailbox }
import Header._
import ContentType._
import com.minosiants.pencil.data.Body.{ Ascii, Html, Utf8 }
import com.minosiants.pencil.protocol.Encoding.{ `7bit`, `base64` }
import scodec.bits.BitVector
import Email._
import Command._
import com.minosiants.pencil.protocol.Code._

final case class Request(email: Email, socket: SmtpSocket)

object Smtp {

  def apply[A](run: Request => IO[A]): Smtp[A] =
    Kleisli(req => run(req))

  def pure[A](a: A): Smtp[A] =
    Kleisli.pure(a)

  def liftF[A](a: IO[A]): Smtp[A] =
    Kleisli.liftF(a)

  def local[A](f: Request => Request)(smtp: Smtp[A]): Smtp[A] =
    Kleisli.local(f)(smtp)

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
    req.email.recipients.toList.traverse(rcptCommand)
  }

  def data(): Smtp[Replies] = command(Data)

  def rset(): Smtp[Replies] = command(Rset)

  def vrfy(str: String): Smtp[Replies] = command(Vrfy(str))

  def noop(): Smtp[Replies] = command(Noop)

  def quit(): Smtp[Replies] = command(Quit)

  def text(txt: String): Smtp[Unit] = write(const(Text(txt)))

  def startTls(): Smtp[Replies] = command(StartTls)

  def authLogin(): Smtp[Replies] =
    for {
      rep <- command(AuthLogin)
      _   <- checkReplyFor(`334`, rep)
    } yield rep

  def checkReplyFor(code: Code, replies: Replies): Smtp[Replies] =
    liftF(
      IO.fromEither(
        Either.cond(
          replies.hasCode(code),
          replies,
          Error
            .SmtpError(s"don't have ${code.value} in replies. ${replies.show}")
        )
      )
    )
  def login(credentials: Credentials): Smtp[Unit] =
    for {
      _ <- authLogin()
      ru <- command(
        Text(s"${credentials.username.show.toBase64} ${Command.end}")
      )
      _ <- checkReplyFor(`334`, ru)
      rp <- command(
        Text(s"${credentials.password.show.toBase64} ${Command.end}")
      )
      _ <- checkReplyFor(`235`, rp)
    } yield ()

  def endEmail(): Smtp[Replies] = Smtp { req =>
    val p = req.email match {
      case TextEmail(_, _, _, _, _, _) => text(Command.endEmail) >> read
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
      case TextEmail(_, _, _, _, _, Some(Ascii(body))) =>
        (text(s"$body ${Command.end}") >> endEmail()).run(req)
      case _ => Error.smtpError("Body is not ascii")
    }
  }

  def subjectHeader(): Smtp[Option[Unit]] = Smtp { req =>
    req.email match {
      case TextEmail(_, _, _, _, Some(Subject(sub)), _) =>
        text(s"Subject: $sub ${Command.end}").run(req).map(Some(_))
      case MimeEmail(_, _, _, _, Some(Subject(sub)), _, _, _) =>
        text(s"Subject: =?utf-8?b?${sub.toBase64}?= ${Command.end}")
          .run(req)
          .map(Some(_))
      case _ => IO(None)
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
  ): Smtp[Unit] = text(s"${headerShow.show(ct)} ${Command.end}")

  def contentTransferEncoding(encoding: Encoding): Smtp[Unit] =
    text(
      s"${headerShow.show(`Content-Transfer-Encoding`(encoding))} ${Command.end}"
    )

  def boundary(isFinal: Boolean = false): Smtp[Unit] = Smtp { req =>
    req.email match {
      case e @ MimeEmail(_, _, _, _, _, _, _, Boundary(b)) =>
        val end = if (isFinal) "--" else ""
        if (e.isMultipart)
          text(s"--$b$end ${Command.end}").run(req)
        else
          IO(())
      case TextEmail(_, _, _, _, _, _) => Error.smtpError("not mime")
    }
  }

  def mimePart(body: String, mech: Encoding, ct: `Content-Type`): Smtp[Unit] =
    for {
      _ <- boundary()
      _ <- contentTypeHeader(ct)
      _ <- contentTransferEncoding(mech)
      _ <- text(Command.end)
      _ <- text(s"$body ${Command.end}")
    } yield ()

  def multipart(): Smtp[Unit] = Smtp { req =>
    req.email match {
      case m @ MimeEmail(_, _, _, _, _, _, _, Boundary(b)) if m.isMultipart =>
        contentTypeHeader(
          `Content-Type`(`multipart/mixed`, Map("boundary" -> b))
        ).run(req)
      case m @ MimeEmail(_, _, _, _, _, _, _, _) => IO(())
      case _                                     => Error.smtpError("Does not support multipart")
    }

  }
  def mimeBody(): Smtp[Unit] = Smtp { req =>
    req.email match {
      case MimeEmail(_, _, _, _, _, Some(Ascii(body)), _, _) =>
        mimePart(
          body,
          `7bit`,
          `Content-Type`(`text/plain`, Map("charset" -> "US-ASCII"))
        ).run(req)

      case MimeEmail(_, _, _, _, _, Some(Html(body)), _, _) =>
        mimePart(
          body.toBase64,
          `base64`,
          `Content-Type`(`text/html`, Map("charset" -> "UTF-8"))
        ).run(req)

      case MimeEmail(_, _, _, _, _, Some(Utf8(body)), _, _) =>
        mimePart(
          body.toBase64,
          `base64`,
          `Content-Type`(`text/plain`, Map("charset" -> "UTF-8"))
        ).run(req)

      case _ => Error.smtpError("not mime email")
    }

  }

  def attachments(): Smtp[Unit] = Smtp { req =>
    req.email match {
      case TextEmail(_, _, _, _, _, _) =>
        Error.smtpError("attachments not supported")
      case MimeEmail(_, _, _, _, _, _, attach, _) =>
        val result = attach.map { a =>
          val res = Files.inputStream(a.file)
          for {
            encoded <- res.use(v => IO(BitVector.fromInputStream(v).toBase64))
            ct      <- res.use(ContentTypeFinder.findType)
            _ <- mimePart(
              encoded,
              `base64`,
              `Content-Type`(ct, Map("name" -> a.file.getFileName.toString))
            ).run(req)
          } yield ()
        }
        result.sequence.as(())
    }
  }

}
