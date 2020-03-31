package com.minosiants.pencil
package protocol

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scodec.{Attempt, Codec, DecodeResult}
import scodec.bits._
import scodec.codecs._

import scala.io.Source
import org.specs2.matcher.MatchResult

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

    "reply encoding" in forAll(replyGen)(property(Reply.codec))

    "replies encoding" in forAll(repliesGen)(property(Replies.codec))

    "mailbox encoding" in forAll(mailboxGen)(property(MailboxCodec.codec))

    "command encoding" in forAll(commandGen)(property(Command.codec))

  }

  def property[A](codec:Codec[A])(a:A): MatchResult[Attempt[DecodeResult[A]]]= {
    val encoded = codec.encode(a)
    val decoded = encoded.flatMap(codec.decode)
    decoded ==== Attempt.successful(DecodeResult(a, BitVector.empty))
  }
}
