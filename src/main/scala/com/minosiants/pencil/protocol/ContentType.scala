/*
 * Copyright 2020 Kaspar Minosiants
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.minosiants.pencil
package protocol

import cats.Show

enum ContentType(val mainType: String, val subType: String):
  case `text/plain` extends ContentType("text", "plain")
  case `text/html`  extends ContentType("text", "html")
  case `image/png`  extends ContentType("image", "png")
  case `image/gif`  extends ContentType("image", "gif")
  case `image/jpeg` extends ContentType("image", "jpeg")
  case `audio/mpeg` extends ContentType("audio", "mpeg")
  case `audio/wav`  extends ContentType("audio", "wav")
  case `audio/aac`  extends ContentType("audio", "wav")
  case `video/mpeg` extends ContentType("video", "mpeg")
  case `video/ogg`  extends ContentType("video", "ogg")
  case `video/webm` extends ContentType("video", "webm")
  case `application/octet-stream`
      extends ContentType("application", "octet-stream")
  case `application/json`      extends ContentType("application", "json")
  case `application/pdf`       extends ContentType("application", "pdf")
  case `multipart/mixed`       extends ContentType("multipart", "mixed")
  case `multipart/alternative` extends ContentType("multipart", "alternative")
  case `multipart/digest`      extends ContentType("multipart", "digest")

object ContentType:
  def findType(str: String): Option[ContentType] =
    str.split("/") match {
      case Array(main, sub) =>
        ContentType.values.find(ct => ct.mainType == main && ct.subType == sub)
      case _ => None
    }
  given Show[ContentType] =
    Show(ct => s"${ct.mainType.toLowerCase}/${ct.subType.toLowerCase}")
