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

import java.nio.charset.StandardCharsets

import scodec.bits.{BitVector, ByteVector}

package pencil:
  val CRLF: ByteVector = ByteVector("\r\n".getBytes)
  implicit class ExtraStringOps(private val str: String) extends AnyVal {
    def toBase64: String       = toBitVector.toBase64
    def toBitVector: BitVector = BitVector(str.getBytes(StandardCharsets.UTF_8))
    def toByteVector: ByteVector = toBitVector.bytes
  }
  type To = com.minosiants.pencil.data.ToType.To
  val To = com.minosiants.pencil.data.ToType.To
  type From = com.minosiants.pencil.data.FromType.From
  val From = com.minosiants.pencil.data.FromType.From
  type Cc = com.minosiants.pencil.data.CcType.Cc
  val Cc = com.minosiants.pencil.data.CcType.Cc
  type Bcc = com.minosiants.pencil.data.BccType.Bcc
  val Bcc = com.minosiants.pencil.data.BccType.Bcc
  type Subject = com.minosiants.pencil.data.SubjectType.Subject
  val Subject = com.minosiants.pencil.data.SubjectType.Subject
  type Attachment = com.minosiants.pencil.data.AttachmentType.Attachment
  val Attachment = com.minosiants.pencil.data.AttachmentType.Attachment
  type Host = com.minosiants.pencil.data.HostType.Host
  val Host = com.minosiants.pencil.data.HostType.Host
  type Boundary = com.minosiants.pencil.data.BoundaryType.Boundary
  val Boundary = com.minosiants.pencil.data.BoundaryType.Boundary
