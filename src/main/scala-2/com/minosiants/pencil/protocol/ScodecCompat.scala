package com.minosiants.pencil.protocol

import scodec.bits.BitVector
import scodec.{Attempt, Codec}

trait ScodecCompat {
  implicit class ScodecCodecCompat[A](codec: Codec[A]) {
    def encodeAll(values: List[A]): Attempt[BitVector] = {
      Codec.encodeSeq(codec)(values)
    }
  }
}
