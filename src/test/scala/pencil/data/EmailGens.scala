package pencil
package data

import java.nio.file.{Path, Paths}

import org.scalacheck.Gen
import org.scalacheck.Arbitrary

trait EmailGens {

  val localPartCharGen: Gen[Char] = Gen
    .choose(33.toChar, 126.toChar)
    .retryUntil(v => !MailboxParser.special.contains(v))

  val localPartGen: Gen[String] = Gen
    .nonEmptyListOf(localPartCharGen)
    .map(_.take(200).mkString)
    .retryUntil(v => !v.contains(".."))

  val domainGen: Gen[String] = for {
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

  val mailboxGen: Gen[Mailbox] = for {
    lp <- localPartGen
    domain <- domainGen
  } yield Mailbox.unsafeFromString(s"$lp@$domain")

  given Arbitrary[From] = Arbitrary(mailboxGen.map(From(_)))
  given Arbitrary[To] = Arbitrary(Gen.nonEmptyListOf(mailboxGen).map(To(_*)))
  given Arbitrary[Cc] = Arbitrary(Gen.nonEmptyListOf(mailboxGen).map(Cc(_*)))
  given Arbitrary[Bcc] = Arbitrary(Gen.nonEmptyListOf(mailboxGen).map(Bcc(_*)))
  given Arbitrary[Subject] = Arbitrary(Gen.asciiPrintableStr.map(Subject.apply))
  given Arbitrary[Body.Ascii] = Arbitrary(
    Gen.asciiPrintableStr.map(Body.Ascii.apply)
  )
  given Arbitrary[Body.Html] = Arbitrary(
    Gen.asciiPrintableStr.map(Body.Html.apply)
  )
  given Arbitrary[Body.Utf8] = Arbitrary(
    Gen.asciiPrintableStr.map(Body.Utf8.apply)
  )
  given Arbitrary[Path] = Arbitrary(Gen.asciiPrintableStr.map(Paths.get(_)))
  given Arbitrary[Attachment] = Arbitrary(
    Arbitrary.arbitrary[Path].map(Attachment(_))
  )
  given Arbitrary[List[Attachment]] = Arbitrary(
    Gen.listOf(Arbitrary.arbitrary[Attachment])
  )

  val emailTypeGen: Gen[EmailType] = {
    val textGen = Gen.const(EmailType.Text)
    val mimeGen = for {
      boundary <- Gen.asciiPrintableStr.map(Boundary(_))
      attachments <- Gen.listOf(Arbitrary.arbitrary[Attachment])
    } yield EmailType.Mime(boundary, attachments)
    Gen.oneOf(textGen, mimeGen)
  }

  given Arbitrary[Email] = Arbitrary {
    for {
      from <- Arbitrary.arbitrary[From]
      to <- Arbitrary.arbitrary[To]
      cc <- Gen.option(Arbitrary.arbitrary[Cc])
      bcc <- Gen.option(Arbitrary.arbitrary[Bcc])
      subject <- Gen.option(Arbitrary.arbitrary[Subject])
      emailType <- emailTypeGen
      body <- emailType.fold(
        Gen.option(Arbitrary.arbitrary[Body.Ascii]),
        _ =>
          Gen.option(
            Gen.oneOf(
              Arbitrary.arbitrary[Body.Html],
              Arbitrary.arbitrary[Body.Utf8]
            )
          )
      )
    } yield Email(from, to, cc, bcc, subject, body, emailType)
  }
}
