package com.minosiants.pencil
package protocol

import cats.Show

final case class ContentType(mainType: String, subType: String)
    extends Product
    with Serializable

object ContentType {
  lazy val `text/plain`: ContentType = ContentType("text", "plain")
  lazy val `text/html`: ContentType  = ContentType("text", "html")
  lazy val `image/png`: ContentType  = ContentType("image", "png")
  lazy val `image/gif`: ContentType  = ContentType("image", "gif")
  lazy val `image/jpeg`: ContentType = ContentType("image", "jpeg")
  lazy val `audio/mpeg`: ContentType = ContentType("audio", "mpeg")
  lazy val `audio/wav`: ContentType  = ContentType("audio", "wav")
  lazy val `audio/aac`: ContentType  = ContentType("audio", "wav")
  lazy val `video/mpeg`: ContentType = ContentType("video", "mpeg")
  lazy val `video/ogg`: ContentType  = ContentType("video", "ogg")
  lazy val `video/webm`: ContentType = ContentType("video", "webm")
  lazy val `application/octet-stream`: ContentType =
    ContentType("application", "octet-stream")
  lazy val `application/json`: ContentType = ContentType("application", "json")
  lazy val `multipart/mixed`: ContentType  = ContentType("multipart", "mixed")
  lazy val `multipart/alternative`: ContentType =
    ContentType("multipart", "alternative")
  lazy val `multipart/digest`: ContentType = ContentType("multipart", "digest")

  lazy val allTypes = List(
    `text/plain`,
    `text/html`,
    `image/gif`,
    `image/jpeg`,
    `image/png`,
    `audio/aac`,
    `audio/mpeg`,
    `audio/wav`,
    `video/mpeg`,
    `video/ogg`,
    `video/webm`,
    `application/json`,
    `application/octet-stream`,
    `multipart/alternative`,
    `multipart/digest`,
    `multipart/mixed`
  )

  def findType(str: String): Option[ContentType] =
    str.split("/") match {
      case Array(main, sub) =>
        allTypes.find(ct => ct.mainType == main && ct.subType == sub)
      case _ => None
    }

  implicit lazy val contentTypeShow: Show[ContentType] = Show(
    ct => s"${ct.mainType.toLowerCase}/${ct.subType.toLowerCase}"
  )
}