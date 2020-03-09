package com.minosiants.pencil
package protocol

import cats.Show
import cats.syntax.show._

sealed trait Header extends Product with Serializable

object Header {

  final case class `MIME-Version`(value: String = "1.0") extends Header

  final case class `Content-Type`(
      contentType: ContentType,
      params: Map[String, String] = Map.empty
  ) extends Header

  final case class `Content-Transfer-Encoding`(mechanism: Encoding)
      extends Header

  implicit lazy val headerShow: Show[Header] = Show.show {

    case `MIME-Version`(value) => s"MIME-Version: $value"
    case `Content-Type`(ct, params) =>
      val values = (ct.show :: params.foldRight(List.empty[String]) {
        (item, acc) =>
          acc :+ s"${item._1}=${item._2}"
      }).mkString(";")
      s"Content Type: $values"
    case `Content-Transfer-Encoding`(mechanism) =>
      s"Content-Transfer-Encoding: ${mechanism.show}"

  }
}
//Character set
//Message
//Entity
//Body part
//Body
//7bit Data
//8bit Data
//Binary Data
//Lines
