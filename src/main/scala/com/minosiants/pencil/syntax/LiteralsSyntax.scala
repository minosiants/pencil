package com.minosiants.pencil
package syntax

import com.minosiants.pencil.data._

trait LiteralsSyntax {
  implicit def pencilLiteralsSyntax(sc: StringContext): LiteralsOps =
    new LiteralsOps(sc)
}

class LiteralsOps(val sc: StringContext) extends AnyVal {
  def mailbox(args: Any*): Mailbox =
    macro LiteralSyntaxMacros.mailboxInterpolator

  def subject(args: Any*): Subject = Subject(sc.toString)

  def attachment(args: Any*): Attachment =
    macro LiteralSyntaxMacros.attachmentInterpolator

}
