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

import java.time.{ Instant, ZoneId, ZoneOffset }
import java.time.format.DateTimeFormatter
import java.util.UUID

import cats._
import cats.data.Kleisli
import cats.effect.{ ContextShift, Sync }
import cats.implicits._
import com.minosiants.pencil.data.Body.{ Ascii, Html, Utf8 }
import com.minosiants.pencil.data.Email._
import com.minosiants.pencil.data.{ Email, Mailbox, _ }
import com.minosiants.pencil.protocol.Code._
import com.minosiants.pencil.protocol.Command._
import com.minosiants.pencil.protocol.ContentType._
import com.minosiants.pencil.protocol.Encoding.{ `7bit`, `base64` }
import com.minosiants.pencil.protocol.Header._
import com.minosiants.pencil.protocol._
import fs2.io.file.readAll
import fs2.{ Chunk, Stream }

import scala.Function._
object Smtp {
  // Used for easier type inference
  def apply[F[_]]: SmtpPartiallyApplied[F] =
    new SmtpPartiallyApplied(dummy = true)

  private[pencil] final class SmtpPartiallyApplied[F[_]](
      private val dummy: Boolean
  ) extends AnyVal {
    def apply[A](run: Request[F] => F[A]): Smtp[F, A] =
      Kleisli(req => run(req))
  }

  def pure[F[_]: Applicative, A](a: A): Smtp[F, A] =
    Kleisli.pure(a)

  def unit[F[_]: Applicative]: Smtp[F, Unit] = pure(())

  def liftF[F[_], A](a: F[A]): Smtp[F, A] =
    Kleisli.liftF(a)

  def local[F[_], A](
      f: Request[F] => Request[F]
  )(smtp: Smtp[F, A]): Smtp[F, A] =
    Kleisli.local(f)(smtp)

  def write[F[_]](run: Email => Command): Smtp[F, Unit] = Smtp[F] { req =>
    req.socket.write(run(req.email))
  }

  def ask[F[_]: Applicative]: Smtp[F, Request[F]] = Kleisli.ask[F, Request[F]]

  def processErrors[F[_]: ApplicativeError[*[_], Throwable]](
      replies: Replies
  ): F[Replies] =
    if (replies.success) Applicative[F].pure(replies)
    else Error.smtpError[F, Replies](replies.show)

  def read[F[_]: MonadError[*[_], Throwable]]: Smtp[F, Replies] =
    Smtp[F](_.socket.read()).flatMapF(processErrors[F])

  def command1[F[_]: MonadError[*[_], Throwable]](
      run: Email => Command
  ): Smtp[F, Replies] =
    write[F](run).flatMap(_ => read[F])

  def command[F[_]: MonadError[*[_], Throwable]](c: Command): Smtp[F, Replies] =
    command1(const(c))

  def init[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] = read[F]

  def ehlo[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    command(Ehlo(Host.local().name))

  def mail[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    command1(m => Mail(m.from.box))

  def rcpt[F[_]: MonadError[*[_], Throwable]](): Smtp[F, List[Replies]] =
    Smtp[F] { req =>
      val rcptCommand = (m: Mailbox) => command[F](Rcpt(m)).run(req)
      req.email.recipients.toList.traverse(rcptCommand)
    }

  def data[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    command(Data)

  def rset[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    command(Rset)

  def vrfy[F[_]: MonadError[*[_], Throwable]](str: String): Smtp[F, Replies] =
    command(Vrfy(str))

  def noop[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    command(Noop)

  def quit[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    command(Quit)

  def text[F[_]](txt: String): Smtp[F, Unit] = write(const(Text(txt)))

  def startTls[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    command(StartTls)

  def authLogin[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    for {
      rep <- command[F](AuthLogin)
      _   <- checkReplyFor[F](`334`, rep)
    } yield rep

  def checkReplyFor[F[_]: ApplicativeError[*[_], Throwable]](
      code: Code,
      replies: Replies
  ): Smtp[F, Replies] =
    liftF(
      ApplicativeError[F, Throwable].fromEither(
        Either.cond(
          replies.hasCode(code),
          replies,
          Error
            .SmtpError(s"don't have ${code.value} in replies. ${replies.show}")
        )
      )
    )

  def login[F[_]: MonadError[*[_], Throwable]](
      credentials: Credentials
  ): Smtp[F, Unit] =
    for {
      _ <- authLogin[F]()
      ru <- command[F](
        Text(s"${credentials.username.show.toBase64} ${Command.end}")
      )
      _ <- checkReplyFor[F](`334`, ru)
      rp <- command[F](
        Text(s"${credentials.password.show.toBase64} ${Command.end}")
      )
      _ <- checkReplyFor[F](`235`, rp)
    } yield ()

  def endEmail[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    Smtp[F] { req =>
      val p = req.email match {
        case TextEmail(_, _, _, _, _, _) =>
          text[F](Command.endEmail).flatMap(_ => read[F])
        case _ =>
          for {
            _ <- boundary[F](true)
            _ <- text[F](Command.endEmail)
            r <- read[F]
          } yield r
      }
      p.run(req)
    }

  def asciiBody[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    Smtp[F] { req =>
      req.email match {
        case TextEmail(_, _, _, _, _, Some(Ascii(body))) =>
          (text(s"$body${Command.end}").flatMap(_ => endEmail[F]())).run(req)

        case _ =>
          Error.smtpError[F, Replies]("Body is not ascii")
      }
    }

  def subjectHeader[F[_]: MonadError[*[_], Throwable]]()
      : Smtp[F, Option[Unit]] = Smtp[F] { req =>
    req.email match {
      case TextEmail(_, _, _, _, Some(Subject(sub)), _) =>
        text(s"Subject: $sub${Command.end}").run(req).map(Some(_))

      case MimeEmail(_, _, _, _, Some(Subject(sub)), _, _, _) =>
        text(s"Subject: =?utf-8?b?${sub.toBase64}?=${Command.end}")
          .run(req)
          .map(Some(_))

      case _ =>
        Applicative[F].pure(None)
    }
  }

  def fromHeader[F[_]](): Smtp[F, Unit] = Smtp[F] { req =>
    text(s"From: ${req.email.from.show}${Command.end}").run(req)
  }

  def toHeader[F[_]](): Smtp[F, Unit] = Smtp[F] { req =>
    text(s"To: ${req.email.to.show}${Command.end}").run(req)
  }

  def ccHeader[F[_]: Applicative](): Smtp[F, Option[Unit]] = Smtp[F] { req =>
    req.email.cc match {
      case Some(v) =>
        text(s"Cc: ${v.show}${Command.end}").run(req).map(Some(_))

      case None =>
        Applicative[F].pure(None)
    }
  }

  def bccHeader[F[_]: Applicative](): Smtp[F, Option[Unit]] = Smtp[F] { req =>
    req.email.bcc match {
      case Some(v) =>
        text(s"Bcc: ${v.show}${Command.end}").run(req).map(Some(_))

      case None =>
        Applicative[F].pure(None)
    }
  }

  val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("EEE, d MMM yyyy HH:mm:ss Z (z)")
    .withZone(ZoneId.from(ZoneOffset.UTC))

  def dateHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] = Smtp[F] {
    req =>
      val time = dateFormatter.format(req.timestamp)
      text(s"Date: ${time}${Command.end}").run(req)
  }

  def messageIdHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    Smtp[F] { req =>
      val hostName = req.host.name
      val seconds  = req.timestamp.getEpochSecond
      val uuid     = UUID.randomUUID().toString
      text(s"Message-ID: <$uuid.$seconds@$hostName${Command.end}>").run(req)
    }

  def mainHeaders[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    for {
      _ <- dateHeader[F]()
      _ <- fromHeader[F]()
      _ <- toHeader[F]()
      _ <- ccHeader[F]()
      _ <- bccHeader[F]()
      _ <- subjectHeader[F]()
      _ <- messageIdHeader[F]()
    } yield ()

  def mimeHeader[F[_]](): Smtp[F, Unit] = {
    text(s"${headerShow.show(`MIME-Version`())}${Command.end}")
  }

  def contentTypeHeader[F[_]](
      ct: `Content-Type`
  ): Smtp[F, Unit] = text(s"${headerShow.show(ct)}${Command.end}")

  def contentTransferEncoding[F[_]](encoding: Encoding): Smtp[F, Unit] =
    text(
      s"${headerShow.show(`Content-Transfer-Encoding`(encoding))}${Command.end}"
    )

  def boundary[F[_]: ApplicativeError[*[_], Throwable]](
      isFinal: Boolean = false
  ): Smtp[F, Unit] = Smtp[F] { req =>
    req.email match {
      case e @ MimeEmail(_, _, _, _, _, _, _, Boundary(b)) =>
        val end = if (isFinal) "--" else ""
        if (e.isMultipart)
          text(s"--$b$end${Command.end}").run(req)
        else
          Applicative[F].unit

      case TextEmail(_, _, _, _, _, _) =>
        Error.smtpError[F, Unit]("not mime")
    }
  }

  def mimePart[F[_]: MonadError[*[_], Throwable]](
      mech: Encoding,
      ct: `Content-Type`
  ): Smtp[F, Unit] =
    for {
      _ <- boundary[F]()
      _ <- contentTypeHeader[F](ct)
      _ <- contentTransferEncoding[F](mech)
      _ <- text(Command.end)
    } yield ()

  def multipart[F[_]: ApplicativeError[*[_], Throwable]](): Smtp[F, Unit] =
    Smtp[F] { req =>
      req.email match {
        case m @ MimeEmail(_, _, _, _, _, _, _, Boundary(b)) if m.isMultipart =>
          contentTypeHeader(
            `Content-Type`(`multipart/mixed`, Map("boundary" -> b))
          ).run(req)

        case MimeEmail(_, _, _, _, _, _, _, _) =>
          Applicative[F].unit

        case _ =>
          Error.smtpError[F, Unit]("Does not support multipart")
      }
    }

  def lines[F[_]: Applicative](txt: String): Smtp[F, Unit] = Smtp[F] { req =>
    txt
      .grouped(76)
      .toList
      .traverse_ { ln =>
        text(s"$ln${Command.end}").run(req)
      }
  }

  def mimeBody[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] = Smtp[F] {
    req =>
      req.email match {
        case MimeEmail(_, _, _, _, _, Some(Ascii(body)), _, _) =>
          mimePart[F](
            `7bit`,
            `Content-Type`(`text/plain`, Map("charset" -> "US-ASCII"))
          ).flatMap(_ => lines[F](body))
            .run(req)

        case MimeEmail(_, _, _, _, _, Some(Html(body)), _, _) =>
          mimePart[F](
            `base64`,
            `Content-Type`(`text/html`, Map("charset" -> "UTF-8"))
          ).flatMap(_ => lines[F](body.toBase64)).run(req)

        case MimeEmail(_, _, _, _, _, Some(Utf8(body)), _, _) =>
          mimePart[F](
            `base64`,
            `Content-Type`(`text/plain`, Map("charset" -> "UTF-8"))
          ).flatMap(_ => lines[F](body.toBase64)).run(req)

        case _ =>
          Error.smtpError[F, Unit]("not mime email")
      }
  }

  def attachments[F[_]: Sync: ContextShift: Applicative](): Smtp[F, Unit] = {
    Smtp[F] { req =>
      req.email match {
        case TextEmail(_, _, _, _, _, _) =>
          Error.smtpError[F, Unit]("attachments not supported")

        case MimeEmail(_, _, _, _, _, _, attachments, _) =>
          attachments.traverse_ { a =>
            val attachment = a.file
            for {
              ct <- Files
                .inputStream[F](attachment)
                .use(ContentTypeFinder.findType[F])
              _ <- mimePart[F](
                `base64`,
                `Content-Type`(
                  ct,
                  Map(
                    "name" -> s"=?utf-8?b?${attachment.getFileName.toString.toBase64}?="
                  )
                )
              ).run(req)
              _ <- readAll[F](attachment, req.blocker, 1024)
                .through(fs2.text.base64.encode)
                .flatMap(s => Stream.chunk(Chunk.chars(s.toCharArray)))
                .chunkN(n = 76)
                .map(chunk => chunk.iterator.mkString)
                .evalMap(line => text(s"${line}${Command.end}").run(req))
                .compile
                .drain
            } yield ()
          }
      }
    }
  }
}
