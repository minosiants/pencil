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

import cats.implicits._
import java.io.InputStream
import java.nio.file.{ Path, Paths, Files => JFiles }

import cats.effect.{ Sync, Resource }
import com.minosiants.pencil.data.Error

import Function._

object Files {

  def inputStream[F[_]: Sync](file: Path): Resource[F, InputStream] = {
    Resource
      .make {
        Sync[F]
          .delay {
            JFiles.newInputStream(file)
          }
          .handleErrorWith(
            const(Error.resourceNotFound[F, InputStream](file.toString))
          )
      } { is =>
        if (is != null)
          Sync[F].delay(is.close())
        else
          Error.resourceNotFound[F, Unit](file.toString)
      }
  }

  def pathFrom[F[_]: Sync](file: String): F[Either[Error, Path]] =
    Sync[F].delay {
      val path = Paths.get(file)
      Either.cond(JFiles.exists(path), path, Error.ResourceNotFound(file))
    }

  def pathFromClassLoader[F[_]: Sync](file: String): F[Either[Error, Path]] =
    Sync[F].delay {
      val resource = getClass.getClassLoader.getResource(file)
      val cond     = resource != null && JFiles.exists(Paths.get(resource.toURI))
      Either.cond(cond, Paths.get(resource.toURI), Error.ResourceNotFound(file))
    }
}
