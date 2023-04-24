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

package pencil

import cats.implicits._
import java.io.InputStream
import java.nio.file.{Path, Paths, Files => JFiles}

import cats.MonadError
import cats.MonadThrow
import cats.effect.{Resource, Sync}
import pencil.data.Error
import Function._

object Files:
  def inputStream[F[_]: Sync](file: Path): Resource[F, InputStream] =
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
        if is != null then Sync[F].delay(is.close())
        else Error.resourceNotFound[F, Unit](file.toString)
      }

  def pathFrom[F[_]: MonadThrow](file: String): F[Path] =
    MonadError[F, Throwable].ensure {
      Paths.get(file).pure[F]
    } {
      Error.ResourceNotFound(file)
    } { path =>
      JFiles.exists(path)
    }

  def pathFromClassLoader[F[_]: Sync](file: String): F[Path] =
    Sync[F]
      .delay {
        getClass.getClassLoader.getResource(file)
      }
      .flatMap { resource =>
        if resource != null && JFiles.exists(Paths.get(resource.toURI)) then
          Paths.get(resource.toURI).pure[F]
        else Error.resourceNotFound[F, Path](file)
      }
      .handleErrorWith { _ =>
        Error.resourceNotFound[F, Path](file)
      }
