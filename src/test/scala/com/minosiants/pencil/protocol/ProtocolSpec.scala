package com.minosiants.pencil.protocol

import org.specs2.mutable.Specification
import scodec.Attempt
import scodec.bits._
import scodec.codecs._

import scala.io.Source

class ProtocolSpec extends Specification {
  "Protocol" should {
    "decode response" in {

      val is       = getClass().getResourceAsStream("/output.txt")
      val output   = Source.fromInputStream(is).mkString
      val expected = output.split("\r\n").toList

      val vec = ascii.encode(output).getOrElse(BitVector.empty)

      Reply.codecReplies.decode(vec).map(_.value) match {
        case Attempt.Successful(result) =>
          result mustEqual expected
        case Attempt.Failure(cause) =>
          true mustEqual false

      }
    }

  }
}
