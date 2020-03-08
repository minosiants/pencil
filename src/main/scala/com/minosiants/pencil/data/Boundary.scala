package com.minosiants.pencil
package data

import java.security.MessageDigest

final case class Boundary(value:String) extends Product with Serializable

object Boundary {
  def genFrom(value:String):Boundary = {
    val cs = MessageDigest.getInstance("MD5")
      .digest(value.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
    Boundary(cs)
  }
}
