package com.minosiants
package pencil.protocol

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

  def success: Boolean = replies.forall(_.code.success)
}

object Replies {
  implicit lazy val RepliesShow: Show[Replies] = Show.fromToString
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

  implicit val replyCodec: Codec[Reply] = (
    ("code" | codeCodec) ::
      ("sep" | limitedSizeBits(8, ascii)) ::
      ("text" | ascii)
  ).as[Reply]

  val CRLF: BitVector = ascii.encode("\r\n").getOrElse(BitVector.empty)

  implicit val repliesCodec: Codec[Replies] = (
    ("replies" | DelimiterListCodec(CRLF, replyCodec))
  ).as[Replies]
}