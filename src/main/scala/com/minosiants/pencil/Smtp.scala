package com.minosiants.pencil

import cats.data.Kleisli
import cats.effect.IO
import protocol._
import cats.syntax.show._
import scala.Function._

final case class Request(email: Email, socket: SmtpSocket) {}

case class Response(value: String)

object Smtp {

  def apply[A](run: Request => IO[A]): Smtp[A] =
    Kleisli(req => run(req))

  def command(run: Email => Command) = Smtp { req =>
    val resp = for {
      _ <- req.socket.write(run(req.email))
      r <- req.socket.read()
    } yield r

    resp.flatMap(r => if (r.success) IO(r) else Error.smtpError(r.show))
  }

  def init(): Smtp[Replies] = Smtp { req =>
    req.socket
      .read()
      .flatMap(r => if (r.success) IO(r) else Error.smtpError(r.show))

  }
  def ehlo(): Smtp[Replies] = command(const(Ehlo("pencil")))


  // def email():Smtp[Replies] = command()

}
