package com.minosiants.pencil
package protocol

import cats.effect.IO
import org.specs2.mutable.Specification
import scodec.Attempt
import scodec.bits._
import scodec.codecs._
import cats.implicits._
import data._
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
    "bla" in {
      val nums = List(3, 5, 8, 15)

      val r: Either[List[Int], List[Int]] = nums.foldM(List.empty[Int]) {
        case (acc, c) if c < 8 => (c :: acc).asRight
        case (acc, _)          => acc.asLeft
      }

      println(r)

      val l: List[IO[Unit]] = List(
        IO(println("1")),
        IO(println("2")),
        Error.smtpError("error"),
        IO(println("3"))
      )

      println(l.sequence.attempt.unsafeRunSync())
      success
    }

  }
}
