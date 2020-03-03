package com.minosiants
package pencil.protocol

import scodec.Attempt.Successful
import scodec.bits._
import scodec.codecs._
import scodec.{ Attempt, Codec, DecodeResult, Err }

final case class Reply(code: Code, sep: String, text: String)

object Reply {

  implicit val codeCodec = Codec[Code](
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

  implicit val codecReply = (
    ("code" | codeCodec) ::
      ("sep" | limitedSizeBits(8, ascii)) ::
      ("text" | ascii)
  ).as[Reply]

  val del = ascii.encode("\r\n").getOrElse(BitVector.empty)

  implicit val codecReplies: Codec[List[Reply]] = listDelimited(del, codecReply)
}
