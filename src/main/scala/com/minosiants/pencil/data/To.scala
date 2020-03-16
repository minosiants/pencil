package com.minosiants.pencil
package data

import cats.Show

final case class To(boxes: List[Mailbox]) extends Product with Serializable

object To {

  implicit lazy val toShow: Show[To] = Show.show(
    to => to.boxes.map(v => Mailbox.mailboxShow.show(v)).mkString(",")
  )
  def apply(to: Mailbox*): To = To(to.toList)
}
