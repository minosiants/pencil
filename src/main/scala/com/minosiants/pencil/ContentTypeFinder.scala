package com.minosiants.pencil

import java.io.InputStream

import protocol._
import data._

import cats.effect.IO
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

}
