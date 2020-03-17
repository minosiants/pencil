package com.minosiants.pencil
package data

import cats.Show

final case class Username(value: String) extends Product with Serializable
final case class Password(value: String) extends Product with Serializable
final case class Credentials(username: Username, password: Password)
    extends Product
    with Serializable

object Username {
  implicit lazy val usernameShow: Show[Username] = Show.show(_.value)
}

object Password {
  implicit lazy val passwordShow: Show[Password] = Show.show(_.value)
}
