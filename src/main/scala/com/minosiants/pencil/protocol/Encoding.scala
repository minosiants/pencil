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
