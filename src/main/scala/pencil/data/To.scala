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

package pencil.data

import cats.Show
import cats.data.NonEmptyList
import cats.kernel.Semigroup
import cats.syntax.show.*
object ToType:

  opaque type To = NonEmptyList[Mailbox]

  object To:

    def apply(to: Mailbox*): To = NonEmptyList.fromListUnsafe(to.toList)
    extension (self: To)
      def +(that: To): To = self ::: that
      def mailboxes: NonEmptyList[Mailbox] = self
      def toList: List[Mailbox] = self.toList

    given Show[To] = Show.show(to => to.map(_.show).toList.mkString(","))

    given Semigroup[To] =
      Semigroup.instance((a, b) => a + b)
