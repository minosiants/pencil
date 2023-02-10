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

package com.minosiants.pencil.data

import cats._
// import cats.effect.IO

import scala.util.control.NoStackTrace

sealed trait Error extends NoStackTrace with Product with Serializable

object Error {

  final case class SmtpError(msg: String)           extends Error
  final case class AuthError(msg: String)           extends Error
  final case class InvalidMailBox(msg: String)      extends Error
  final case class UnableCloseResource(msg: String) extends Error
  final case class ResourceNotFound(msg: String)    extends Error
  final case class TikaException(msg: String)       extends Error

  implicit lazy val errorShow: Show[Error] = Show.show {
    case SmtpError(msg)           => s"Smtp error: $msg "
    case AuthError(msg)           => s"Auth error: $msg"
    case InvalidMailBox(msg)      => s"Invalid maildbox: $msg"
    case UnableCloseResource(msg) => s"Unable close resource: $msg"
    case ResourceNotFound(msg)    => s"Resource not found: $msg"
    case TikaException(msg)       => s"Tika exception: $msg"
  }

  def smtpError[F[_]: ApplicativeThrow, A](msg: String): F[A] =
    ApplicativeError[F, Throwable].raiseError[A](SmtpError(msg))

  def authError[F[_]: ApplicativeThrow, A](msg: String): F[A] =
    ApplicativeError[F, Throwable].raiseError[A](AuthError(msg))

  def unableCloseResource[F[_]: ApplicativeThrow, A](
      msg: String
  ): F[A] =
    ApplicativeError[F, Throwable].raiseError(UnableCloseResource(msg))

  def resourceNotFound[F[_]: ApplicativeThrow, A](
      msg: String
  ): F[A] =
    ApplicativeError[F, Throwable].raiseError(ResourceNotFound(msg))

  def tikaException[F[_]: ApplicativeThrow, A](
      msg: String
  )(e: Throwable): F[A] = {
    val m = if (e.getMessage != null) s"Message: ${e.getMessage}" else ""
    ApplicativeError[F, Throwable].raiseError(TikaException(s"$msg $m"))
  }
}
