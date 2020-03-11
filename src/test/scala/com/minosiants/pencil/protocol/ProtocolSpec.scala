package com.minosiants.pencil
package protocol

import cats.effect.IO
import org.specs2.mutable.Specification
import scodec.{ Attempt, DecodeResult }
import scodec.bits._
import scodec.codecs._
import cats.implicits._
import data._

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
  }
}
