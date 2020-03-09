package com.minosiants.pencil

import java.io.File

import cats.effect.{ IO, Resource }
import scodec.bits.BitVector

object Files {

  def resource(file: File): Resource[IO, BitVector] = {
    Resource
      .make {
        IO {
          getClass().getResourceAsStream(file.getAbsolutePath)
        }
      } { is =>
        IO(is.close()).handleErrorWith(
          _ => data.Error.unableCloseResource(file.getAbsolutePath)
        )
      }
      .map(BitVector.fromInputStream(_))
  }
}
