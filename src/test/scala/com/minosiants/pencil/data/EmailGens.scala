package com.minosiants.pencil.data

import java.nio.file.{Path, Paths}

import com.minosiants.pencil.data.Email.{MimeEmail, TextEmail}
import org.scalacheck.Gen
import FromType.From
import CcType.Cc
import ToType.To
import BccType.Bcc
import AttachmentType.Attachment

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
    lp     <- localPartGen
    domain <- domainGen
  } yield Mailbox.unsafeFromString(s"$lp@$domain")

  val fromGen: Gen[From]           = mailboxGen.map(From(_))
  val toGen: Gen[To]               = Gen.nonEmptyListOf(mailboxGen).map(To(_*))
  val ccGen: Gen[Cc]               = Gen.nonEmptyListOf(mailboxGen).map(Cc(_*))
  val bccGen: Gen[Bcc]             = Gen.nonEmptyListOf(mailboxGen).map(Bcc(_*))
  val subjectGen: Gen[Subject]     = Gen.asciiPrintableStr.map(Subject.apply)
  val textBodyGen: Gen[Body.Ascii] = Gen.asciiPrintableStr.map(Body.Ascii.apply)
  val htmlBodyGen: Gen[Body.Html]  = Gen.asciiPrintableStr.map(Body.Html.apply)
  val utf8BodyGen: Gen[Body.Utf8]  = Gen.asciiPrintableStr.map(Body.Utf8.apply)
  val pathGen: Gen[Path]           = Gen.asciiPrintableStr.map(Paths.get(_))
  val attachmentGen: Gen[Attachment]        = pathGen.map(Attachment(_))
  val attachmentsGen: Gen[List[Attachment]] = Gen.listOf(attachmentGen)

  val textEmailGen: Gen[TextEmail] = for {
    from    <- fromGen
    to      <- toGen
    cc      <- Gen.option(ccGen)
    bcc     <- Gen.option(bccGen)
    subject <- Gen.option(subjectGen)
    body    <- Gen.option(textBodyGen)
  } yield TextEmail(from, to, cc, bcc, subject, body)

  def mimeEmailGen(bodyGen: Gen[Body]): Gen[Email.MimeEmail] =
    for {
      from        <- fromGen
      to          <- toGen
      cc          <- Gen.option(ccGen)
      bcc         <- Gen.option(bccGen)
      subject     <- Gen.option(subjectGen)
      body        <- Gen.option(bodyGen)
      attachments <- attachmentsGen
      boundary    <- Gen.asciiPrintableStr.map(Boundary(_))
    } yield MimeEmail(from, to, cc, bcc, subject, body, attachments, boundary)

  val utf8EmailGen: Gen[MimeEmail] = mimeEmailGen(utf8BodyGen)
}
