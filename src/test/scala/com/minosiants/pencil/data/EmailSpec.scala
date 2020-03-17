package com.minosiants.pencil
package data

import com.minosiants.pencil.data.Email.TextEmail
import org.specs2.mutable.Specification

import EmailSpec._

class EmailSpec extends Specification {
  "Email" should {
    "TextEmail setCc" in {
      val newCc = cc"he@me.com"
      textEmail.setCc(newCc).cc must beSome(newCc)
    }

    "TextEmail addCc" in {
      val cc1 = cc"he@me.com"
      val cc2 = cc"he2@me.com"

      textEmail.addCc(cc1).addCc(cc2).cc must beSome(cc1 + cc2)
      textEmail.addCc(cc1.boxes.head).addCc(cc2.boxes.head).cc must beSome(
        cc1 + cc2
      )
    }
    "TextEmail setBcc" in {
      val newBcc = bcc"he@me.com"
      textEmail.setBcc(newBcc).bcc must beSome(newBcc)
    }
    "TextEmail addBcc" in {
      val bcc1 = bcc"he@me.com"
      val bcc2 = bcc"he2@me.com"
      textEmail.addBcc(bcc1).addBcc(bcc2).bcc must beSome(bcc1 + bcc2)
      textEmail.addBcc(bcc1.boxes.head).addBcc(bcc2.boxes.head).bcc must beSome(
        bcc1 + bcc2
      )
    }
    "TextEmail setBody" in {
      val newbody = Body.Ascii("hello2")
      textEmail.setBody(newbody).body must beSome(newbody)
    }
    "TextEmail setSubject" in {
      val newSubject = subject"new one"
      textEmail.setSubject(newSubject).subject must beSome(newSubject)
    }

    "TextEmail setFrom" in {
      val newFrom = from"new@one.com"
      textEmail.setFrom(newFrom).from must be(newFrom)
    }

    "TextEmail setTo" in {
      val newTo = to"new@one.com"
      textEmail.setTo(newTo).to must be(newTo)
    }

    "TextEmail addTo" in {
      val newTo = to"he@me.com"
      textEmail.addTo(newTo).to must beEqualTo(textEmail.to + newTo)
    }
  }

}

object EmailSpec {
  val textEmail: TextEmail = {
    Email.text(
      from"user1@mydomain.tld",
      to"user1@example.com",
      subject"first email",
      Body.Ascii("hello")
    )
  }
}
