package com.minosiants.pencil
package data

import org.specs2.mutable.Specification

import org.specs2.ScalaCheck
import org.scalacheck._
import cats.syntax.semigroup._
import cats.instances.option._

class MimeEmailSpec extends Specification with ScalaCheck with EmailGens {

  "MimeEmail" should {

    "setCc" in Prop.forAll(utf8EmailGen, ccGen) { (email, cc) =>
      email.setCc(cc).cc ==== Some(cc)
    }

    "addCc" in Prop.forAll(utf8EmailGen, ccGen) { (email, cc) =>
      email.addCc(cc).cc ==== (email.cc |+| Some(cc))

    }

    "setBcc" in Prop.forAll(utf8EmailGen, bccGen) { (email, bcc) =>
      email.setBcc(bcc).bcc ==== Some(bcc)
    }

    "addBcc" in Prop.forAll(utf8EmailGen, bccGen) { (email, bcc) =>
      email.addBcc(bcc).bcc ==== (email.bcc |+| Some(bcc))
    }

    "setBody as text" in Prop.forAll(utf8EmailGen, textBodyGen) {
      (email, body) =>
        email.setBody(body).body ==== Some(body)
    }
    "setBody as html" in Prop.forAll(utf8EmailGen, htmlBodyGen) {
      (email, body) =>
        email.setBody(body).body ==== Some(body)
    }

    "setSubject" in Prop.forAll(utf8EmailGen, subjectGen) { (email, subject) =>
      email.setSubject(subject).subject ==== Some(subject)
    }

    "setFrom" in Prop.forAll(utf8EmailGen, fromGen) { (email, from) =>
      email.setFrom(from).from ==== from
    }

    "setTo" in Prop.forAll(utf8EmailGen, toGen) { (email, to) =>
      email.setTo(to).to ==== to
    }

    "addTo" in Prop.forAll(utf8EmailGen, toGen) { (email, to) =>
      email.addTo(to).to ==== (email.to |+| to)
    }
    "addAttachment" in Prop.forAll(utf8EmailGen, attachmentGen) {
      (email, attachment) =>
        email
          .addAttachment(attachment)
          .attachments ==== (email.attachments :+ attachment)
    }

  }
}
