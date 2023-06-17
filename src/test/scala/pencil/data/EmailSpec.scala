package pencil
package data

import org.specs2.mutable.Specification

import org.specs2.ScalaCheck
import org.scalacheck.*
import org.scalacheck.Prop.*
import cats.syntax.semigroup.*
import cats.instances.option.*

class EmailSpec extends Specification with ScalaCheck with EmailGens:

  "Email" should {

    "setCc" in forAll { (email: Email, cc: Cc) =>
      email.setCc(cc).cc ==== Some(cc)
    }

    "addCc" in forAll { (email: Email, cc: Cc) =>
      email.addCc(cc).cc ==== (email.cc |+| Some(cc))
    }

    "setBcc" in forAll { (email: Email, bcc: Bcc) =>
      email.setBcc(bcc).bcc ==== Some(bcc)
    }

    "addBcc" in forAll { (email: Email, bcc: Bcc) =>
      email.addBcc(bcc).bcc ==== (email.bcc |+| Some(bcc))
    }

    "setBody as text" in forAll { (email: Email, body: Body.Ascii) =>
      email.setBody(body).body ==== Some(body)
    }
    "setBody as html" in forAll { (email: Email, body: Body.Html) =>
      email.setBody(body).body ==== Some(body)
    }

    "setSubject" in forAll { (email: Email, subject: Subject) =>
      email.setSubject(subject).subject ==== Some(subject)
    }

    "setFrom" in forAll { (email: Email, from: From) =>
      email.setFrom(from).from ==== from
    }

    "setTo" in forAll { (email: Email, to: To) =>
      email.setTo(to).to ==== to
    }

    "addTo" in forAll { (email: Email, to: To) =>
      email.addTo(to).to ==== (email.to |+| to)
    }
    "addAttachment" in forAll { (email: Email, attachment: Attachment) =>
      if email.isMime then email.addAttachment(attachment) == (email + attachment)
      else true
    }
  }
