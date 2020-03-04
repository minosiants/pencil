package com.minosiants
package pencil.protocol

final case class Code(value: Int, description: String)
    extends Product
    with Serializable {
  def success: Boolean = value < 400
}

object Code {

  def code(value: Int): Option[Code] = codes.find(_.value == value)

  val `214` = Code(214, "Help message")
  val `211` = Code(211, "System status, or system help reply")
  val `220` = Code(220, "Service ready")
  val `221` = Code(221, "Service closing transmission channel")
  val `250` = Code(250, "Requested mail action okay, completed")
  val `251` = Code(251, "User not local; will forward to <forward-path>")
  val `252` =
    Code(252, "Cannot VRFY user, but will accept message and attempt delivery")
  val `354` = Code(354, "Start mail input; end with <CRLF>.<CRLF")
  val `421` =
    Code(421, "<domain> Service not available, closing transmission channel")
  val `450` = Code(450, "Requested mail action not taken: mailbox unavailable")
  val `451` = Code(451, "Requested action aborted: local error in processing")
  val `452` =
    Code(452, "Requested action not taken: insufficient system storage")
  val `455` = Code(455, "Server unable to accommodate parameters")
  val `500` = Code(500, "Syntax error, command unrecognized")
  val `501` = Code(501, "Syntax error in parameters or arguments")
  val `502` = Code(502, "Command not implemented")
  val `503` = Code(503, "Bad sequence of commands")
  val `504` = Code(504, "Command parameter not implemented")
  val `550` = Code(550, "Requested action not taken: mailbox unavailable")
  val `551` = Code(551, "User not local; please try <forward-path>")
  val `552` =
    Code(552, "Requested mail action aborted: exceeded storage allocation")
  val `553` = Code(553, "Requested action not taken: mailbox name not allow")
  val `554` = Code(554, "Transaction failed")
  val `555` =
    Code(555, "MAIL FROM/RCPT TO parameters not recognized or not implemented")

  val codes = List(
    `214`,
    `211`,
    `220`,
    `221`,
    `250`,
    `251`,
    `252`,
    `354`,
    `421`,
    `450`,
    `451`,
    `452`,
    `455`,
    `500`,
    `501`,
    `502`,
    `503`,
    `504`,
    `550`,
    `551`,
    `552`,
    `553`,
    `554`,
    `555`
  )

}
