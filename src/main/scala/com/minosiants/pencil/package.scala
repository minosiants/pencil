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

package com.minosiants

import cats.data.Kleisli
import cats.effect.IO
import com.minosiants.pencil.syntax.LiteralsSyntax
import scodec.bits.BitVector
import scodec.codecs._

package object pencil extends LiteralsSyntax {

  type Smtp[A] = Kleisli[IO, Request, A]

  val CRLF: BitVector = ascii.encode("\r\n").getOrElse(BitVector.empty)

  implicit class ExtraStringOps(str: String) {
    def toBase64: String = {
      BitVector.view(str.getBytes()).toBase64
    }
    def toBitVector: BitVector = {
      ascii.encode(str).getOrElse(BitVector.empty)
    }
  }

}
