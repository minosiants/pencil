package com.minosiants.pencil
package data

import cats.Show
import cats.syntax.show._

final case class From(box: Mailbox) extends Product with Serializable

object From {
  implicit lazy val fromShow: Show[From] = Show.show(
    from => s"${from.box.show}"
  )
}
