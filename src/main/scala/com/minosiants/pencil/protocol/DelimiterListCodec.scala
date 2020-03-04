package com.minosiants.pencil
package protocol

import scodec.{ Attempt, Codec, DecodeResult, SizeBound }
import scodec.bits.BitVector

final case class DelimiterListCodec[A](
    delimiter: BitVector,
    valueCodec: Codec[A]
) extends Codec[List[A]] {

  override def decode(bits: BitVector): Attempt[DecodeResult[List[A]]] = {

    def go(vec: BitVector): Attempt[List[A]] = {
      val index = vec.indexOfSlice(delimiter)
      if (index < 0)
        Attempt.successful(List.empty[A])
      else {
        val (value, tail) = vec.splitAt(index)
        valueCodec.decode(value) match {
          case Attempt.Successful(DecodeResult(a, _)) =>
            go(tail.drop(delimiter.size)).map(v => a :: v)
          case Attempt.Failure(cause) => Attempt.Failure(cause)
        }
      }
    }

    go(bits).map(DecodeResult(_, BitVector.empty))

  }

  override def encode(value: List[A]): Attempt[BitVector] =
    valueCodec.encodeAll(value)

  override def sizeBound: SizeBound = SizeBound.unknown
}
