package com.minosiants.pencil
package data

import org.specs2.mutable.Specification

import org.specs2.ScalaCheck
import org.scalacheck._
import cats.syntax.semigroup._
import cats.instances.option._

class TextEmailSpec extends Specification with ScalaCheck with EmailGens {

  "TextEmail" should {

    "setCc" in Prop.forAll(textEmailGen, ccGen) { (email, cc) =>
      email.setCc(cc).cc ==== Some(cc)
    }

    "addCc" in Prop.forAll(textEmailGen, ccGen) { (email, cc) =>
      email.addCc(cc).cc ==== (email.cc |+| Some(cc))

    }

    "setBcc" in Prop.forAll(textEmailGen, bccGen) { (email, bcc) =>
      email.setBcc(bcc).bcc ==== Some(bcc)
    }

    "addBcc" in Prop.forAll(textEmailGen, bccGen) { (email, bcc) =>
      email.addBcc(bcc).bcc ==== (email.bcc |+| Some(bcc))
    }

    "setBody" in Prop.forAll(textEmailGen, textBodyGen) { (email, body) =>
      email.setBody(body).body ==== Some(body)
    }

    "setSubject" in Prop.forAll(textEmailGen, subjectGen) { (email, subject) =>
      email.setSubject(subject).subject ==== Some(subject)
    }

    "setFrom" in Prop.forAll(textEmailGen, fromGen) { (email, from) =>
      email.setFrom(from).from ==== from
    }

    "setTo" in Prop.forAll(textEmailGen, toGen) { (email, to) =>
      email.setTo(to).to ==== to
    }

    "addTo" in Prop.forAll(textEmailGen, toGen) { (email, to) =>
      email.addTo(to).to ==== (email.to |+| to)
    }
  }

}
