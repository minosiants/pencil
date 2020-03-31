/*
 * Copyright 2020 Kaspar Minosiants
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.minosiants.pencil
package protocol

import scodec.{ Attempt, Codec, DecodeResult, SizeBound }
import scodec.bits.{ BitVector, ByteVector }

final case class DelimiterListCodec[A](
    delimiter: ByteVector,
    valueCodec: Codec[A]
) extends Codec[List[A]] {

  override def decode(bits: BitVector): Attempt[DecodeResult[List[A]]] = {

    def go(vec: ByteVector): Attempt[List[A]] = {
      val index = vec.indexOfSlice(delimiter)
      if (index < 0)
        Attempt.successful(List.empty[A])
      else {
        val (value, tail) = vec.splitAt(index)
        valueCodec.decode(value.bits) match {
          case Attempt.Successful(DecodeResult(a, _)) =>
            go(tail.drop(delimiter.size)).map(v => a :: v)
          case Attempt.Failure(cause) => Attempt.Failure(cause)
        }
      }
    }

    go(bits.toByteVector).map(DecodeResult(_, BitVector.empty))

  }

  override def encode(value: List[A]): Attempt[BitVector] =
    valueCodec.encodeAll(value)

  override def sizeBound: SizeBound = SizeBound.unknown
}
