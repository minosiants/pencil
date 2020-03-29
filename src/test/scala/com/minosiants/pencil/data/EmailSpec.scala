package com.minosiants.pencil
package data

import org.specs2.mutable.Specification

import org.specs2.ScalaCheck
import org.scalacheck._
import cats.implicits._

class EmailSpec extends Specification with ScalaCheck with Gens {

  "Email" should {
    "TextEmail setCc" in Prop.forAll(textEmailGen, ccGen) { (email, cc) =>
      email.setCc(cc).cc === Some(cc)
    }

    "TextEmail addCc" in Prop.forAll(textEmailGen, ccGen) { (email, cc) =>
      email.addCc(cc).cc === email.cc.combine(Some(cc))

    }

    "TextEmail setBcc" in Prop.forAll(textEmailGen, bccGen) { (email, bcc) =>
      email.setBcc(bcc).bcc === Some(bcc)
    }

    "TextEmail addBcc" in Prop.forAll(textEmailGen, bccGen) { (email, bcc) =>
      email.addBcc(bcc).bcc === email.bcc.combine(Some(bcc))
    }

    "TextEmail setBody" in Prop.forAll(textEmailGen, textBodyGen) {
      (email, body) =>
        email.setBody(body).body === Some(body)
    }

    "TextEmail setSubject" in Prop.forAll(textEmailGen, subjectGen) {
      (email, subject) =>
        email.setSubject(subject).subject === Some(subject)
    }

    "TextEmail setFrom" in Prop.forAll(textEmailGen, fromGen) { (email, from) =>
      email.setFrom(from).from === from
    }

    "TextEmail setTo" in Prop.forAll(textEmailGen, toGen) { (email, to) =>
      email.setTo(to).to === to
    }

    "TextEmail addTo" in Prop.forAll(textEmailGen, toGen) { (email, to) =>
      email.addTo(to).to === email.to.combine(to)
    }
  }

}
