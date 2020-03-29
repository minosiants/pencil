package com.minosiants.pencil
package data

import org.specs2.mutable.Specification
import org.scalacheck._
import org.specs2.ScalaCheck

class MailboxParserSpec extends Specification with ScalaCheck {
  import MailboxParserSpec._

  "MailboxParser" should {

    "parse" in Prop.forAll(localPartGen, domainGen) { (lp, domain) =>
      val result = MailboxParser.parse(s"$lp@$domain")
      result == Right(Mailbox(lp, domain))
    }

    "parse with invalid localPart" in Prop.forAll(
      invalidLocalPartGen,
      domainGen
    ) { (lp, domain) =>
      val result = MailboxParser.parse(s"$lp@$domain")
      result.isLeft
    }

    "parse with invalid domain" in Prop.forAll(
      localPartGen,
      invalidDomainGen
    ) { (lp, domain) =>
      val result = MailboxParser.parse(s"$lp@$domain")
      result.isLeft
    }
  }
}

object MailboxParserSpec extends EmailGens {
  val localPartWithDots: Gen[String] = Gen
    .nonEmptyListOf(
      Gen.frequency((1, localPartGen.map(_.take(10))), (3, Gen.const("..")))
    )
    .map(_.mkString)
    .retryUntil(_.contains(".."))

  val localPartWithSpecialCharsGen: Gen[String] = Gen
    .nonEmptyListOf(Gen.asciiChar.retryUntil(MailboxParser.special.contains))
    .filter(_.length > 1)
    .map(_.mkString)

  val localPartWithControlCharsGen: Gen[Char] = Gen.choose(0.toChar, 31.toChar)

  val invalidLocalPartGen: Gen[Any] = Gen.oneOf(
    localPartWithDots,
    localPartWithSpecialCharsGen,
    localPartWithControlCharsGen
  )

  val invalidDomainGen: Gen[String] = Gen.oneOf(
    domainGen.map(v => "-" + v),
    domainGen.map(v => v + "-"),
    domainGen.map(v => "-" + v + "-")
  )
}
