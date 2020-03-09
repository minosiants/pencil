package com.minosiants.pencil

import protocol._
import data._

import java.io.File

import cats.effect.{ IO, Resource }
import org.apache.tika.Tika

object ContentTypeFinder {

  lazy val tika = new Tika()

  def findType(file: File): IO[ContentType] =
    Resource
      .make {
        IO(getClass().getResourceAsStream(file.getAbsolutePath))
      } { is =>
        if (is != null)
          IO(is.close())
        else
          Error.resourceNotFound(file.getAbsolutePath)
      }
      .use { is =>
        IO {
          val ct = tika.detect(is)
          ContentType
            .findType(ct)
            .getOrElse(ContentType.`application/octet-stream`)
        }
      }

}
