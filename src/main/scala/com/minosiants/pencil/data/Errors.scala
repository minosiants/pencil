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

import cats.Show
import cats.effect.IO
import org.apache.tika.exception.TikaException

import scala.util.control.NoStackTrace

sealed trait Error extends NoStackTrace with Product with Serializable

object Error {

  final case class SmtpError(msg: String)           extends Error
  final case class InvalidMailBox(msg: String)      extends Error
  final case class UnableCloseResource(msg: String) extends Error
  final case class ResourceNotFound(msg: String)    extends Error
  final case class TikaException(msg: String)       extends Error

  implicit lazy val errorShow: Show[Error] = Show.show {
    case SmtpError(msg)           => s"Smtp error: $msg "
    case InvalidMailBox(msg)      => s"Invalid maildbox: $msg"
    case UnableCloseResource(msg) => s"Unable close resource: $msg"
    case ResourceNotFound(msg)    => s"Resource not found: $msg"
    case TikaException(msg)       => s"Tika exception: $msg"
  }

  def smtpError[A](msg: String): IO[A] =
    IO.raiseError[A](SmtpError(msg))

  def unableCloseResource[A](msg: String): IO[A] =
    IO.raiseError(UnableCloseResource(msg))

  def resourceNotFound[A](msg: String): IO[A] =
    IO.raiseError(ResourceNotFound(msg))

  def tikaException[A](msg: String)(e: Throwable): IO[A] = {
    val m = if (e.getMessage != null) s"Message: ${e.getMessage}" else ""
    IO.raiseError(TikaException(s"$msg $m"))
  }
}
