package com.minosiants.pencil.data

import cats.Show
import cats.effect.IO

import scala.util.control.NoStackTrace

sealed trait Error extends NoStackTrace with Product with Serializable

final case class SmtpError(msg: String) extends Error

object Error {

  implicit lazy val errorShow:Show[Error] = Show.show {
    case SmtpError(msg) => s"Smtp error: $msg "
  }

  def smtpError[A](msg: String): IO[A] =
    IO.raiseError[A](SmtpError(msg))
}
