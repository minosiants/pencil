package com.minosiants.pencil.data

import cats.effect.IO

import scala.util.control.NoStackTrace

sealed trait Error extends NoStackTrace with Product with Serializable

final case class SmtpError(msg: String) extends Error

object Error {

  def smtpError[A](msg: String): IO[A] =
    IO.raiseError[A](SmtpError(msg))
}
