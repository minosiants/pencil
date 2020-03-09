package com.minosiants.pencil
package protocol

import com.minosiants.pencil.data.Mailbox

sealed trait Header extends Product with Serializable

object Header {

  final case class `MIME-Version`(value: String) extends Header

  final case class `Content-Type`(
      contentType: ContentType,
      params: Map[String, String]
  ) extends Header

  final case class `Content-Transfer-Encoding`(mechanism: Encoding)
      extends Header

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
