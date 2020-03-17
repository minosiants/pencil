package com.minosiants.pencil
package data

import cats.Show
import cats.syntax.show._
import cats.data.NonEmptyList

final case class Cc(boxes: NonEmptyList[Mailbox])
    extends Product
    with Serializable {
  def +(cc: Cc) = copy(boxes = boxes ::: cc.boxes)
}

object Cc {
  def apply(boxes: Mailbox*): Cc =
    new Cc(NonEmptyList.fromListUnsafe(boxes.toList))

  implicit lazy val ccShow: Show[Cc] = Show.show(
    cc => cc.boxes.map(_.show).toList.mkString(",")
  )
}
