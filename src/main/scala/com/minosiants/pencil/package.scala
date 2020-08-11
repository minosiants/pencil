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

import java.nio.charset.{ Charset, StandardCharsets }
import java.util.Base64

import cats.data.Kleisli
import com.minosiants.pencil.syntax.LiteralsSyntax
import scodec.bits.{ BitVector, ByteVector }

package object pencil extends LiteralsSyntax {

  type Smtp[F[_], A] = Kleisli[F, Request[F], A]

  implicit class ExtraStringOps(str: String) {
    def toBase64Mime(charset: Charset): String =
      Base64.getMimeEncoder.encodeToString(str.getBytes(charset))
    def toBase64(charset: Charset): String =
      ByteVector.view(str.getBytes(charset)).toBase64
    def toBase64Ascii: String     = toBase64(StandardCharsets.US_ASCII)
    def toBase64UTF8: String      = toBase64(StandardCharsets.UTF_8)
    def toBase64UTF8Mime: String  = toBase64Mime(StandardCharsets.UTF_8)
    def toBase64AsciiMime: String = toBase64Mime(StandardCharsets.US_ASCII)

    def toBitVector(charset: Charset = StandardCharsets.US_ASCII): BitVector =
      BitVector(str.getBytes(charset))
    def toByteVector(charset: Charset = StandardCharsets.US_ASCII): ByteVector =
      ByteVector(str.getBytes(charset))
  }

  val CRLF: ByteVector = "\r\n".toByteVector()

}
