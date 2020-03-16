package com.minosiants.pencil
package data

import java.nio.file.Path

final case class Attachment(file: Path) extends Product with Serializable

object Attachment {

  def fromString(file: String): Either[Error, Attachment] = {
    Files
      .pathFrom(file)
      .orElse(Files.pathFromClassLoader(file))
      .map(Attachment(_))
  }

  def unsafeFromString(file: String): Attachment =
    fromString(file).fold(throw _, identity)

}
