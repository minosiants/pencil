package com.minosiants.pencil

import cats.data.Kleisli
import cats.effect.IO
import protocol._
import data._
import cats.implicits._

import scala.Function._
import com.minosiants.pencil.data.{ Mail, Mailbox }

final case class Request(email: Mail, socket: SmtpSocket) {}

object Smtp {

  def apply[A](run: Request => IO[A]): Smtp[A] =
    Kleisli(req => run(req))

  def write(run: Mail => Command): Smtp[Unit] = Smtp { req =>
    req.socket.write(run(req.email))
  }

  def read(): Smtp[Replies] = Smtp(_.socket.read()).flatMapF(processErrors)

  def processErrors(replies: Replies): IO[Replies] =
    if (replies.success) IO(replies) else Error.smtpError(replies.show)

  def command1(run: Mail => Command): Smtp[Replies] = write(run) >> read()

  def command(c: Command): Smtp[Replies] = command1(const(c))

  def init(): Smtp[Replies] = read

  def ehlo(): Smtp[Replies] = command(Ehlo("pencil"))

  def mail(): Smtp[Replies] = command1(m => Mail(m.from.box))

  def rcpt(): Smtp[List[Replies]] = Smtp { req =>
    val rcptCommand = (m: Mailbox) => command(Rcpt(m)).run(req)
    val ccValue     = req.email.cc.map(_.boxes).getOrElse(List.empty[Mailbox])

    for {
      to <- req.email.to.boxes.traverse(rcptCommand)
      cc <- ccValue.traverse(rcptCommand)
    } yield to ++ cc

  }

  def data(): Smtp[Replies] = command(Data)

  def rset(): Smtp[Replies] = command(Rset)

  def vrfy(str: String): Smtp[Replies] = command(Vrfy(str))

  def noop(): Smtp[Replies] = command(Noop)

  def quit(): Smtp[Replies] = command(Quit)

  def text(txt: String): Smtp[Unit] = write(const(Text(txt)))

  def endEmail(): Smtp[Replies] = text(Command.endEmail) >> read

  def body(): Smtp[Option[Replies]] = Smtp { req =>
    req.email.body match {
      case Some(Body(b)) => (text(s"$b") >> endEmail()).run(req).map(Some(_))
      case None          => IO(None)
    }
  }

  def subject(): Smtp[Option[Unit]] = Smtp { req =>
    req.email.subject match {
      case Some(Subject(s)) =>
        text(s"Subject: $s ${Command.end}").run(req).map(Some(_))
      case None => IO(None)
    }
  }

  def sendMail(): Smtp[List[Replies]] =
    for {
      e <- ehlo()
      m <- mail()
      r <- rcpt()
      d <- data()
      _ <- subject()
      b <- body()
    } yield e :: m :: r ++ (d :: b.toList)

}
