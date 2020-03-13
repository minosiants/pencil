package com.minosiants.pencil
package protocol

import org.specs2.mutable.Specification
import scodec.DecodeResult
import scodec.bits._
import scodec.codecs._
import com.minosiants.pencil.data.Mailbox
import cats.syntax.show
import scala.io.Source

class ProtocolSpec extends Specification {

  "Protocol" should {

    "decode strings with CRLF into list" in {
      val is     = getClass().getResourceAsStream("/output.txt")
      val output = Source.fromInputStream(is).mkString
      val result =
        DelimiterListCodec(CRLF, ascii).decode(output.toBitVector)
      val expected = output.split("\r\n").toList
      result.toEither must beRight(DecodeResult(expected, BitVector.empty))
    }

    "decode reply" in {
      val result = Reply.replyCodec.decode("250-PIPELINING".toBitVector)
      result.toEither must beRight(
        DecodeResult(
          Reply(Code.code(250).get, "-", "PIPELINING"),
          BitVector.empty
        )
      )
    }

    "decode EHLO command" in {
      val result = Command.codec.decode(command("EHLO domain \r\n")).toEither
      result must beRight(DecodeResult(Ehlo("domain"), BitVector.empty))
    }

    "decode MAIL command" in {
      val box = "name@domain.com"
      val result =
        Command.codec.decode(command(s"MAIL FROM: <$box> \r\n")).toEither
      result must beRight(
        DecodeResult(Mail(Mailbox.unsafeFromString(box)), BitVector.empty)
      )
    }

    "decode RCPT command" in {
      val box = "name2@domain2.com"
      val result =
        Command.codec.decode(command(s"RCPT TO: <$box> \r\n")).toEither
      result must beRight(
        DecodeResult(Rcpt(Mailbox.unsafeFromString(box)), BitVector.empty)
      )
    }

    "decode DATA command" in {
      val result = Command.codec.decode(command(s"DATA: \r\n")).toEither
      result must beRight(DecodeResult(Data, BitVector.empty))
    }

    "decode QUIT command" in {
      val result = Command.codec.decode(command(s"QUIT: \r\n")).toEither
      result must beRight(DecodeResult(Quit, BitVector.empty))
    }
    "encode/decode replies" in {

      val replies = DataSamples.ehloReplies

      val result = Reply.repliesCodec.encode(replies).toEither.flatMap { bits =>
        Reply.repliesCodec.decode(bits).toEither
      }
      println(result)
      result must beRight(DecodeResult(replies, BitVector.empty))
    }
  }

  def command(str: String): BitVector = {
    ascii.encode(str) getOrElse (BitVector.empty)
  }
}
