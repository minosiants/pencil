package com.minosiants.pencil.protocol

import org.specs2.mutable.Specification

import scodec.bits._
import scodec.codecs._

class ProtocolSpec extends Specification {

  "Protocol" should {
    "decode response" in {
      val resp =
        """
            |250-kaspar-air Hello test [127.0.0.1])
            |250-PIPELINING
            |250-ENHANCEDSTATUSCODES
            |250 8BITMIME
            |""".stripMargin

      val resp2 = List(
        "250-kaspar-air Hello test [127.0.0.1])",
        "250-PIPELINING",
        "250-ENHANCEDSTATUSCODES",
        "250 8BITMIME"
      )

      val result = ascii.encode("EHLO test")
      val result2 = ascii
        .encode(resp2.mkString("\r\n"))
        .fold(_ => BitVector.empty, identity)
      println(result2)
      val code =
        ascii.encode("250-PIPELINING").fold(_ => BitVector.empty, identity)
      val code2 = ascii.encode("250").fold(_ => BitVector.empty, identity)

      val r = Reply.codecReplies.decode(result2)
      println(r)
      failure
    }
  }
}
