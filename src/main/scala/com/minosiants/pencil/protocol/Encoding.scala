package com.minosiants.pencil
package protocol

sealed trait Encoding extends Product with Serializable

case object `7bit`             extends Encoding
case object `8bit`             extends Encoding
case object `binary`           extends Encoding
case object `quoted-printable` extends Encoding
case object `base64`           extends Encoding
