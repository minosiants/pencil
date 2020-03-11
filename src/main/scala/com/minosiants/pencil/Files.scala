package com.minosiants.pencil

import java.io.InputStream
import java.nio.file.Path

import cats.effect.{ IO, Resource }
import com.minosiants.pencil.data.Error
import Function._

object Files {

  def inputStream(file: Path): Resource[IO, InputStream] = {
    Resource
      .make {
        IO {
          java.nio.file.Files.newInputStream(file)
        }.handleErrorWith(const(Error.resourceNotFound(file.toString)))
      } { is =>
        if (is != null)
          IO(is.close())
        else
          Error.resourceNotFound(file.toString)
      }
  }
}
