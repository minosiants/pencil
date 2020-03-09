package com.minosiants.pencil

import protocol._
import data._
import java.io.{ File, InputStream }

import cats.effect.{ IO, Resource }
import org.apache.tika.Tika

object ContentTypeFinder {

  lazy val tika = new Tika()

  def findType(is: InputStream): IO[ContentType] =
    IO {
      val ct = tika.detect(is)
      ContentType
        .findType(ct)
        .getOrElse(ContentType.`application/octet-stream`)
    }.handleErrorWith(Error.tikaException("Unable to read input stream"))

  def findType(file: File): IO[ContentType] =
    Files.resource(file).use(findType)

}
