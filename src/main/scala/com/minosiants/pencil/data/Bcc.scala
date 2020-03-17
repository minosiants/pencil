package com.minosiants.pencil
package data

import cats.Show
import cats.syntax.show._

final case class Bcc(boxes: List[Mailbox]) extends Product with Serializable {
  def +(bcc: Bcc) = copy(boxes = boxes ++ bcc.boxes)
}

object Bcc {
  def apply(boxes: Mailbox*): Bcc = new Bcc(boxes.toList)

  implicit lazy val bccShow: Show[Bcc] = Show.show(
    bcc => bcc.boxes.map(_.show).mkString(",")
  )
}
