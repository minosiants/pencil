package com.minosiants.pencil

import org.scalacheck._
import data._
import Email._

trait Gens {
  val localPartCharGen = Gen
    .choose(33.toChar, 126.toChar)
    .retryUntil(v => !MailboxParser.special.contains(v))

  val localPartGen = Gen
    .nonEmptyListOf(localPartCharGen)
    .map(_.take(200).mkString)
    .retryUntil(v => !v.contains(".."))

  val domainGen = for {
    d <- Gen
      .nonEmptyListOf(
        Gen.frequency((1, Gen.numChar), (9, Gen.alphaChar), (1, Gen.const('-')))
      )
      .map(_.take(50))
    ch <- Gen.oneOf(
      Gen.numChar,
      Gen.alphaChar
    )
    domain = (ch :: d) :+ ch
  } yield domain.mkString

  val mailboxGen = for {
    lp     <- localPartGen
    domain <- domainGen
  } yield Mailbox.unsafeFromString(s"$lp@$domain")

  val fromGen     = mailboxGen.map(From(_))
  val toGen       = Gen.nonEmptyListOf(mailboxGen).map(To(_: _*))
  val ccGen       = Gen.nonEmptyListOf(mailboxGen).map(Cc(_: _*))
  val bccGen      = Gen.nonEmptyListOf(mailboxGen).map(Bcc(_: _*))
  val subjectGen  = Gen.asciiPrintableStr.map(Subject(_))
  val textBodyGen = Gen.asciiPrintableStr.map(Body.Ascii(_))

  val textEmailGen = for {
    from    <- fromGen
    to      <- toGen
    cc      <- Gen.option(ccGen)
    bcc     <- Gen.option(bccGen)
    subject <- Gen.option(subjectGen)
    body    <- Gen.option(textBodyGen)
  } yield TextEmail(from, to, cc, bcc, subject, body)

}
