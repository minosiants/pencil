package com.minosiants.pencil
package data

import cats.Show
import cats.data.NonEmptyList

final case class To(boxes: NonEmptyList[Mailbox])
    extends Product
    with Serializable {
  def +(to: To): To = copy(boxes = boxes ::: to.boxes)
}

object To {

  implicit lazy val toShow: Show[To] = Show.show(
    to => to.boxes.map(v => Mailbox.mailboxShow.show(v)).toList.mkString(",")
  )
  def apply(to: Mailbox*): To = To(NonEmptyList.fromListUnsafe(to.toList))
}
