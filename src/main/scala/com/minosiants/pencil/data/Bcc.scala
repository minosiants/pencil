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
package data

import cats.Show
import cats.syntax.show.*
import cats.data.NonEmptyList
import cats.kernel.Semigroup

object BccType:
  opaque type Bcc = NonEmptyList[Mailbox]
  object Bcc:
    def apply(boxes: Mailbox*): Bcc =
      NonEmptyList.fromListUnsafe(boxes.toList)

    extension (self: Bcc)
      def +(bcc: Bcc)                      = self ::: bcc
      def mailboxes: NonEmptyList[Mailbox] = self
      def toList: List[Mailbox]            = self.toList

    given Show[Bcc] = Show.show(bcc => bcc.map(_.show).toList.mkString(","))
    given Semigroup[Bcc] = Semigroup.instance((a, b) => a + b)
