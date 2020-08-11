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
package data

import java.nio.file.Path
import cats.implicits._
import cats.effect.{ Sync, IO }

final case class Attachment(file: Path) extends Product with Serializable

object Attachment {

  def fromString[F[_]: Sync](file: String): F[Either[Error, Attachment]] = {
    Files
      .pathFrom[F](file)
      .flatMap {
        case Left(_)  => Files.pathFromClassLoader[F](file)
        case Right(f) => f.asRight[Error].pure[F]
      }
      .map(_.map(Attachment(_)))
  }

  def unsafeFromString(file: String): Attachment =
    fromString[IO](file).rethrow.unsafeRunSync

}
