package com.minosiants.pencil
package protocol

import cats.Show
import scodec.Attempt.Successful
import scodec.bits._
import scodec.codecs._
import scodec.{ Attempt, Codec, DecodeResult, Err }

final case class Reply(code: Code, sep: String, text: String)
    extends Product
    with Serializable

final case class Replies(replies: List[Reply])
    extends Product
    with Serializable {

  def success: Boolean     = replies.forall(_.code.success)
  def :+(reply: Reply)     = Replies(replies :+ reply)
  def +:(reply: Reply)     = Replies(reply +: replies)
  def ++(replies: Replies) = Replies(this.replies ++ replies.replies)
}

object Replies {
  implicit lazy val RepliesShow: Show[Replies] = Show.fromToString

  def apply(replies: Reply): Replies = Replies(List(replies))

}

object Reply {

  implicit lazy val ReplyShow: Show[Reply] = Show.fromToString

  implicit val codeCodec: Codec[Code] = Codec[Code](
    (value: Code) => ascii.encode(value.value.toString),
    (bits: BitVector) => {
      limitedSizeBits(3 * 8, ascii).decode(bits) match {
        case Successful(DecodeResult(code, rest)) =>
          Attempt.fromOption(
            code.toIntOption
              .flatMap(Code.code)
              .map(c => DecodeResult(c, rest)),
            Err(s"${code} code does not exist")
          )
        case Attempt.Failure(cause) => Attempt.failure(cause)
      }
    }
  )

  val textCodec: Codec[String] = Codec[String](
    { (s: String) =>
      ascii.encode(s + "\r\n")
    }, { bits =>
      {
        if (bits.endsWith(CRLF))
          limitedSizeBits(bits.size - CRLF.size, ascii).decode(bits)
        else
          ascii.decode(bits)
      }
    }
  )

  implicit val replyCodec: Codec[Reply] = (
    ("code" | codeCodec) ::
      ("sep" | limitedSizeBits(8, ascii)) ::
      ("text" | textCodec)
  ).as[Reply]

  implicit val repliesCodec: Codec[Replies] = (
    ("replies" | DelimiterListCodec(CRLF, replyCodec))
  ).as[Replies]
}
