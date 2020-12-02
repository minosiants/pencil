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

  def ask[F[_]: Applicative]: Smtp[F, Request[F]] = Kleisli.ask[F, Request[F]]

  def host[F[_]: MonadError[*[_], Throwable]]: Smtp[F, Host] =
    ask[F].map(_.host)

  def timestamp[F[_]: MonadError[*[_], Throwable]]: Smtp[F, Instant] =
    ask[F].map(_.timestamp)

  def email[F[_]: MonadError[*[_], Throwable]]: Smtp[F, Email] =
    ask[F].map(_.email)

  def socket[F[_]: MonadError[*[_], Throwable]]: Smtp[F, SmtpSocket[F]] =
    ask[F].map(_.socket)

  def write2[F[_]: MonadError[*[_], Throwable]](
      run: Email => Command
  ): Smtp[F, Unit] =
    for {
      mail <- email[F]
      com = run(mail)
      s <- socket[F]
    } yield s.write(com)

  def write[F[_]](run: Email => Command): Smtp[F, Unit] = Smtp[F] { req =>
    req.socket.write(run(req.email))
  }

  def read[F[_]: MonadError[*[_], Throwable]]: Smtp[F, Replies] =
    for {
      s       <- socket[F]
      replies <- liftF(s.read())
      res <- if (replies.success)
        pure[F, Replies](replies)
      else liftF(Error.smtpError[F, Replies](replies.show))
    } yield res

  def processErrors[F[_]: ApplicativeError[*[_], Throwable]](
      replies: Replies
  ): F[Replies] =
    if (replies.success) Applicative[F].pure(replies)
    else Error.smtpError[F, Replies](replies.show)

  def read2[F[_]: MonadError[*[_], Throwable]]: Smtp[F, Replies] =
    Smtp[F](_.socket.read()).flatMapF(processErrors[F])

  def command1_[F[_]: MonadError[*[_], Throwable]](
      run: Email => Command
  ): Smtp[F, Replies] =
    write[F](run).flatMap(_ => read[F])

  def command1[F[_]: MonadError[*[_], Throwable]](
      run: Email => Command
  ): Smtp[F, Replies] =
    for {
      _   <- write[F](run)
      res <- read2[F]
    } yield res

  def command[F[_]: MonadError[*[_], Throwable]](c: Command): Smtp[F, Replies] =
    command1(const(c))

  def init[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] = read[F]

  def ehlo[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    for {
      h       <- host[F]
      replies <- command[F](Ehlo(h.name))
    } yield replies

  def mail[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    for {
      mailbox <- email[F].map(_.from.box)
      replies <- command[F](Mail(mailbox))
    } yield replies

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
    email[F].flatMap {
      case TextEmail(_, _, _, _, _, _) =>
        text[F](Command.endEmail).flatMap(_ => read[F])
      case _ =>
        for {
          _       <- boundary[F](true)
          _       <- text[F](Command.endEmail)
          replies <- read[F]
        } yield replies
    }

  def asciiBody[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Replies] =
    email[F].flatMap {
      case TextEmail(_, _, _, _, _, Some(Ascii(body))) =>
        text(s"$body${Command.end}").flatMap(_ => endEmail[F]())

      case _ =>
        liftF(Error.smtpError[F, Replies]("Body is not ascii"))
    }

  def subjectHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    email[F].flatMap {
      case TextEmail(_, _, _, _, Some(Subject(sub)), _) =>
        text[F](s"Subject: $sub${Command.end}")

      case MimeEmail(_, _, _, _, Some(Subject(sub)), _, _, _) =>
        text[F](s"Subject: =?utf-8?b?${sub.toBase64}?=${Command.end}")

      case _ => unit[F]
    }

  def fromHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    email[F].flatMap(em => text[F](s"From: ${em.from.show}${Command.end}"))

  def toHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    email[F].flatMap(em => text(s"To: ${em.to.show}${Command.end}"))

  def ccHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    email[F].map(_.cc).flatMap {
      case Some(v) =>
        text[F](s"Cc: ${v.show}${Command.end}")

      case None =>
        unit[F]
    }

  def bccHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    email[F].map(_.bcc).flatMap {
      case Some(v) =>
        text[F](s"Bcc: ${v.show}${Command.end}")

      case None =>
        unit[F]
    }

  val dateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("EEE, d MMM yyyy HH:mm:ss Z (z)")
    .withZone(ZoneId.from(ZoneOffset.UTC))

  def dateHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    for {
      ts <- timestamp[F]
      time = dateFormatter.format(ts)
      _ <- text[F](s"Date: ${time}${Command.end}")
    } yield ()

  def messageIdHeader[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    for {
      hostName <- host[F].map(_.name)
      seconds  <- timestamp[F].map(_.getEpochSecond)
      uuid = UUID.randomUUID().toString
      _ <- text[F](s"Message-ID: <$uuid.$seconds@$hostName${Command.end}>")
    } yield ()

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

  def mimeHeader[F[_]](): Smtp[F, Unit] =
    text[F](s"${headerShow.show(`MIME-Version`())}${Command.end}")

  def contentTypeHeader[F[_]](
      ct: `Content-Type`
  ): Smtp[F, Unit] = text[F](s"${headerShow.show(ct)}${Command.end}")

  def contentTransferEncoding[F[_]](encoding: Encoding): Smtp[F, Unit] =
    text[F](
      s"${headerShow.show(`Content-Transfer-Encoding`(encoding))}${Command.end}"
    )

  def boundary[F[_]: MonadError[*[_], Throwable]](
      isFinal: Boolean = false
  ): Smtp[F, Unit] = email[F].flatMap {
    case e @ MimeEmail(_, _, _, _, _, _, _, Boundary(b)) =>
      val end = if (isFinal) "--" else ""
      if (e.isMultipart)
        text(s"--$b$end${Command.end}")
      else
        unit[F]

    case TextEmail(_, _, _, _, _, _) =>
      liftF(Error.smtpError[F, Unit]("not mime"))
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

  def multipart[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    email[F].flatMap {
      case m @ MimeEmail(_, _, _, _, _, _, _, Boundary(b)) if m.isMultipart =>
        contentTypeHeader(
          `Content-Type`(`multipart/mixed`, Map("boundary" -> b))
        )

      case MimeEmail(_, _, _, _, _, _, _, _) =>
        unit[F]

      case _ =>
        liftF(Error.smtpError[F, Unit]("Does not support multipart"))
    }

  def lines[F[_]: Applicative](txt: String): Smtp[F, Unit] = Smtp[F] { req =>
    txt
      .grouped(76)
      .toList
      .traverse_ { ln =>
        text(s"$ln${Command.end}").run(req)
      }
  }

  def mimeBody[F[_]: MonadError[*[_], Throwable]](): Smtp[F, Unit] =
    email[F].flatMap {
      case MimeEmail(_, _, _, _, _, Some(Ascii(body)), _, _) =>
        mimePart[F](
          `7bit`,
          `Content-Type`(`text/plain`, Map("charset" -> "US-ASCII"))
        ).flatMap(_ => lines[F](body))

      case MimeEmail(_, _, _, _, _, Some(Html(body)), _, _) =>
        mimePart[F](
          `base64`,
          `Content-Type`(`text/html`, Map("charset" -> "UTF-8"))
        ).flatMap(_ => lines[F](body.toBase64))

      case MimeEmail(_, _, _, _, _, Some(Utf8(body)), _, _) =>
        mimePart[F](
          `base64`,
          `Content-Type`(`text/plain`, Map("charset" -> "UTF-8"))
        ).flatMap(_ => lines[F](body.toBase64))

      case _ =>
        liftF(Error.smtpError[F, Unit]("not mime email"))
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
