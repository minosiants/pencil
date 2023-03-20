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
import cats.data.NonEmptyList
import cats.kernel.Semigroup

final case class To(boxes: NonEmptyList[Mailbox])
    extends Product
    with Serializable {
  def +(to: To): To = copy(boxes = boxes ::: to.boxes)
}

object To {

  implicit lazy val toShow: Show[To] = Show.show(to =>
    to.boxes.map(v => Mailbox.mailboxShow.show(v)).toList.mkString(",")
  )
  def apply(to: Mailbox*): To = To(NonEmptyList.fromListUnsafe(to.toList))

  implicit lazy val toSemigroup: Semigroup[To] =
    Semigroup.instance((a, b) => a + b)
}
