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
package protocol

import cats.Show

sealed trait Encoding extends Product with Serializable

object Encoding {
  case object `7bit`             extends Encoding
  case object `8bit`             extends Encoding
  case object `binary`           extends Encoding
  case object `quoted-printable` extends Encoding
  case object `base64`           extends Encoding

  implicit lazy val encodingShow: Show[Encoding] = Show.show {
    case `7bit`             => "7bit"
    case `8bit`             => "8bit"
    case `binary`           => "binary"
    case `quoted-printable` => "quoted-printable"
    case `base64`           => "base64"
  }
}
