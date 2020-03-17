package com.minosiants.pencil
package data

import cats.Show
import cats.syntax.show._

final case class Cc(boxes: List[Mailbox]) extends Product with Serializable {
  def +(cc: Cc) = copy(boxes = boxes ++ cc.boxes)
}

object Cc {
  def apply(boxes: Mailbox*): Cc = new Cc(boxes.toList)

  implicit lazy val ccShow: Show[Cc] = Show.show(
    cc => cc.boxes.map(_.show).mkString(",")
  )
}
