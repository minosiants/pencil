package com.minosiants.pencil
package protocol

import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scodec.{ Attempt, DecodeResult }
import scodec.bits._
import scodec.codecs._

import scala.io.Source

class ProtocolSpec extends Specification with ScalaCheck with ProtocolGens {

  "Protocol" should {

    "decode strings with CRLF into list" in {
      val is     = getClass().getResourceAsStream("/output.txt")
      val output = Source.fromInputStream(is).mkString
      val result =
        DelimiterListCodec(CRLF, ascii).decode(output.toBitVector)
      val expected = output.split("\r\n").toList
      result.toEither must beRight(DecodeResult(expected, BitVector.empty))
    }

    "reply encoding" in Prop.forAll(replyGen) { reply =>
      val encoded = Reply.replyCodec.encode(reply)
      val decoded = encoded.flatMap(Reply.replyCodec.decode)
      decoded ==== Attempt.successful(DecodeResult(reply, CRLF))
    }

    "mailbox encoding" in Prop.forAll(mailboxGen) { box =>
      val encoded = MailboxCodec.codec.encode(box)
      val decoded = encoded.flatMap(MailboxCodec.codec.decode)
      decoded ==== Attempt.successful(DecodeResult(box, BitVector.empty))
    }

    "command encoding" in Prop.forAll(commandGen) { command =>
      val encoded = Command.codec.encode(command)
      val decoded = encoded.flatMap(Command.codec.decode)
      decoded ==== Attempt.successful(DecodeResult(command, BitVector.empty))
    }
  }
}
