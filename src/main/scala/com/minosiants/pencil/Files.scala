package com.minosiants.pencil

import java.io.{ File, InputStream }

import cats.effect.{ IO, Resource }
import com.minosiants.pencil.data.Error

object Files {

  def resource(file: File): Resource[IO, InputStream] = {
    Resource
      .make {
        IO {
          getClass().getResourceAsStream(file.getAbsolutePath)
        }
      } {
        { is =>
          if (is != null)
            IO(is.close())
          else
            Error.resourceNotFound(file.getAbsolutePath)
        }
      }
  }
}
