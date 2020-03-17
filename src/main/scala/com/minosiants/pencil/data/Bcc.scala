package com.minosiants.pencil
package data

import cats.Show
import cats.syntax.show._
import cats.data.NonEmptyList

final case class Bcc(boxes: NonEmptyList[Mailbox])
    extends Product
    with Serializable {
  def +(bcc: Bcc) = copy(boxes = boxes ::: bcc.boxes)
}

object Bcc {
  def apply(boxes: Mailbox*): Bcc =
    new Bcc(NonEmptyList.fromListUnsafe(boxes.toList))

  implicit lazy val bccShow: Show[Bcc] = Show.show(
    bcc => bcc.boxes.map(_.show).toList.mkString(",")
  )
}
