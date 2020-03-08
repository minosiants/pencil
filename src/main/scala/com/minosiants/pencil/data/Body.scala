package com.minosiants.pencil
package data

sealed trait Body extends Product with Serializable

object Body {

  final case class Ascii(value: String) extends Body
  final case class Html(value:String) extends Body
  final case class Utf8(value:String) extends Body

}