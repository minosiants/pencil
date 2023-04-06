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

import java.io.InputStream

import protocol._
import data._

import cats.implicits._
import cats.effect.Sync
import org.apache.tika.Tika

object ContentTypeFinder:

  lazy val tika = new Tika()

  def findType[F[_]: Sync](is: InputStream): F[ContentType] =
    Sync[F]
      .delay {
        val ct = tika.detect(is)
        ContentType
          .findType(ct)
          .getOrElse(ContentType.`application/octet-stream`)
      }
      .handleErrorWith(
        Error.tikaException[F, ContentType]("Unable to read input stream")
      )
