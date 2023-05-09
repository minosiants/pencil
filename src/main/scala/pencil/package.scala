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

package pencil

import java.nio.charset.StandardCharsets

import scodec.bits.{BitVector, ByteVector}

val CRLF: ByteVector = ByteVector("\r\n".getBytes)

trait StrOps:
  extension (str: String)
    def toBase64: String = toBitVector.toBase64
    def toBitVector: BitVector = BitVector(str.getBytes(StandardCharsets.UTF_8))
    def toByteVector: ByteVector = toBitVector.bytes

given strOps: StrOps()

type To = pencil.data.ToType.To
val To = pencil.data.ToType.To
type From = pencil.data.FromType.From
val From = pencil.data.FromType.From
type Cc = pencil.data.CcType.Cc
val Cc = pencil.data.CcType.Cc
type Bcc = pencil.data.BccType.Bcc
val Bcc = pencil.data.BccType.Bcc
type Subject = pencil.data.SubjectType.Subject
val Subject = pencil.data.SubjectType.Subject
type Attachment = pencil.data.AttachmentType.Attachment
val Attachment = pencil.data.AttachmentType.Attachment
type Host = pencil.data.HostType.Host
val Host = pencil.data.HostType.Host
type Boundary = pencil.data.BoundaryType.Boundary
val Boundary = pencil.data.BoundaryType.Boundary
type Username = pencil.data.UsernameType.Username
val Username = pencil.data.UsernameType.Username
type Password = pencil.data.PasswordType.Password
val Password = pencil.data.PasswordType.Password
