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

  def to(args: Any*): To = macro LiteralSyntaxMacros.toInterpolator
  def from(args: Any*): From = macro LiteralSyntaxMacros.fromInterpolator
  def cc(args: Any*): Cc = macro LiteralSyntaxMacros.ccInterpolator
  def bcc(args: Any*): Bcc = macro LiteralSyntaxMacros.bccInterpolator
  def subject(args: Any*): Subject = Subject(sc.toString)

  def attachment(args: Any*): Attachment =
    macro LiteralSyntaxMacros.attachmentInterpolator

}
