package pencil
package protocol
import data.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import scodec.{Attempt, Codec, DecodeResult}
import scodec.bits.*
import scodec.codecs.*

import scala.io.Source
import org.specs2.execute.*

class ProtocolSpec extends Specification with ScalaCheck with ProtocolGens {

  "Protocol" should {

    "decode strings with CRLF into list" in {
      val is = getClass().getResourceAsStream("/output.txt")
      val output = Source.fromInputStream(is).mkString
      val result =
        DelimiterListCodec(CRLF, ascii).decode(output.toBitVector)
      val expected = output.split("\r\n").toList
      result.toEither must beRight(DecodeResult(expected, BitVector.empty))
    }

    "code encoding" in forAll(codeGen)(property[Code])

    "reply encoding" in forAll(replyGen)(property[Reply])

    "replies encoding" in forAll(repliesGen)(property[Replies])

    "mailbox encoding" in forAll(mailboxGen)(property[Mailbox])

    "command encoding" in forAll(commandGen)(property[Command])

  }

  def property[A: Codec](a: A): Result = {
    val c = implicitly[Codec[A]]
    val encoded = c.encode(a)
    val decoded = encoded.flatMap(c.decode)
    decoded === Attempt.successful(DecodeResult(a, BitVector.empty))
  }
}
