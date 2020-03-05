package com.minosiants.pencil

import cats.data.Kleisli
import cats.effect.IO
import protocol._
import data._
//import cats.syntax.show._
import cats.implicits._

import scala.Function._
import com.minosiants.pencil.data.{ Email, Mailbox }

final case class Request(email: Email, socket: SmtpSocket) {}

case class Response(value: String)

object Smtp {

  def apply[A](run: Request => IO[A]): Smtp[A] =
    Kleisli(req => run(req))

  def command(run: Email => Command): Smtp[Replies] = Smtp { req =>
    val resp = for {
      _ <- req.socket.write(run(req.email))
      r <- req.socket.read()
    } yield r

    resp.flatMap(r => if (r.success) IO(r) else Error.smtpError(r.show))
  }

  def command(c: Command): Smtp[Replies] = command(const(c))

  def init(): Smtp[Replies] = Smtp { req =>
    req.socket
      .read()
      .flatMap(r => if (r.success) IO(r) else Error.smtpError(r.show))

  }
  def ehlo(): Smtp[Replies] = command(Ehlo("pencil"))

  def mail(): Smtp[Replies] = command(m => Mail(m.from.value))

  def rcpt(): Smtp[List[Replies]] = Smtp { req =>
    val rcptCommand = (m: Mailbox) => command(Rcpt(m)).run(req)
    val cc          = req.email.cc.map(_.value).getOrElse(List.empty[Mailbox])

    for {
      to  <- req.email.to.value.traverse(rcptCommand)
      _cc <- cc.traverse(rcptCommand)
    } yield to ++ _cc

  }

  def data(): Smtp[Replies] = command(Data)

  def rset(): Smtp[Replies] = command(Rset)

  def vrfy(str: String): Smtp[Replies] = command(Vrfy(str))

  def noop(): Smtp[Replies] = command(Noop)

  def quit(): Smtp[Replies] = command(Quit)

  def text(txt: String): Smtp[Replies] = command(Text(txt))

  def body(): Smtp[Replies] = Smtp { req =>
    text(s"${req.email.body} ${Command.endEmail}").run(req)
  }

  def sendMail(): Smtp[List[Replies]] =
    for {
      e <- ehlo()
      m <- mail()
      r <- rcpt()
      d <- data()
      b <- body()
    } yield List(e, m) :: r ++ List(d, b)

}
