package com.minosiants.pencil.data

import cats.Show
import cats.effect.IO

import scala.util.control.NoStackTrace

sealed trait Error extends NoStackTrace with Product with Serializable

object Error {

  final case class SmtpError(msg: String)           extends Error
  final case class InvalidMailBox(msg: String)      extends Error
  final case class UnableCloseResource(msg: String) extends Error
  final case class ResourceNotFound(msg: String)    extends Error

  implicit lazy val errorShow: Show[Error] = Show.show {
    case SmtpError(msg)           => s"Smtp error: $msg "
    case InvalidMailBox(msg)      => s"Invalid maildbox: $msg"
    case UnableCloseResource(msg) => s"Unable close resource: $msg"
    case ResourceNotFound(msg)    => s"Resource not found: $msg"
  }

  def smtpError[A](msg: String): IO[A] =
    IO.raiseError[A](SmtpError(msg))

  def unableCloseResource[A](msg: String): IO[A] =
    IO.raiseError(UnableCloseResource(msg))

  def resourceNotFound[A](msg: String): IO[A] =
    IO.raiseError(ResourceNotFound(msg))
}
