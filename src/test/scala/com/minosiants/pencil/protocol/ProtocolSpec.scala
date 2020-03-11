package com.minosiants.pencil
package protocol

import org.specs2.mutable.Specification
import scodec.DecodeResult
import scodec.bits._
import scodec.codecs._
import com.minosiants.pencil.data.Mailbox

import scala.io.Source

class ProtocolSpec extends Specification {

  "Protocol" should {

    "decode strings with CRLF into list" in {
      val is     = getClass().getResourceAsStream("/output.txt")
      val output = Source.fromInputStream(is).mkString
      val result =
        DelimiterListCodec(Reply.CRLF, ascii).decode(output.toBitVector)
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

    "decode replies" in {
      val resp = List("250-mail.example.com", "250-PIPELINING", "250 8BITMIME")
        .map(_ + "\r\n")
        .mkString

      val result = Reply.repliesCodec.decode(resp.toBitVector)
      result.toEither must beRight(
        DecodeResult(
          Replies(
            List(
              Reply(Code.code(250).get, "-", "mail.example.com"),
              Reply(Code.code(250).get, "-", "PIPELINING"),
              Reply(Code.code(250).get, " ", "8BITMIME")
            )
          ),
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

  }

  def command(str: String): BitVector = {
    ascii.encode(str) getOrElse (BitVector.empty)
  }
}
