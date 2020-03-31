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
import scodec.codecs._
import scodec.Codec

final case class Reply(code: Code, sep: String, text: String)
    extends Product
    with Serializable

final case class Replies(replies: List[Reply])
    extends Product
    with Serializable {

  def success: Boolean             = replies.forall(_.code.success)
  def hasCode(code: Code): Boolean = replies.map(_.code).contains(code)
  def :+(reply: Reply)             = Replies(replies :+ reply)
  def +:(reply: Reply)             = Replies(reply +: replies)
  def ++(replies: Replies)         = Replies(this.replies ++ replies.replies)
}

object Replies {
  implicit lazy val RepliesShow: Show[Replies] = Show.fromToString

  def apply(replies: Reply): Replies = Replies(List(replies))

  implicit val codec: Codec[Replies] = (
    ("replies" | DelimiterListCodec(CRLF, Reply.codec))
  ).as[Replies]

}

object Reply {

  implicit lazy val ReplyShow: Show[Reply] = Show.fromToString

  val textCodec: Codec[String] = Codec[String](
    { (s: String) =>
      ascii.encode(s + "\r\n")
    }, { bits =>
      {
        if (bits.toByteVector.endsWith(CRLF))
          ascii.decode(bits.dropRight(CRLF.bits.size))
        else
          ascii.decode(bits)
      }
    }
  )

  implicit val codec: Codec[Reply] = (
    ("code" | Code.codec) ::
      ("sep" | limitedSizeBits(8, ascii)) ::
      ("text" | textCodec)
  ).as[Reply]

}
