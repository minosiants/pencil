package com.minosiants.pencil
package data

final case class Mailbox(value: String)  extends Product with Serializable
final case class From(box: Mailbox)     extends Product with Serializable
final case class To(boxes: List[Mailbox]) extends Product with Serializable {
  def + (mailbox:Mailbox) = this.copy(boxes :+ mailbox)
}
final case class Cc(boxes: List[Mailbox]) extends Product with Serializable
final case class Subject(value: String)   extends Product with Serializable
final case class Body(value: String)      extends Product with Serializable

final case class Email(
    from: From,
    to: To,
    cc: Option[Cc],
    subject: Option[Subject],
    body: Option[Body]
) extends Product
    with Serializable

object Email {
  def apply(from: From, to: To, subject: Subject, body: Body): Email =
    Email(from, to, None, Some(subject), Some(body))
}

object To {
  def apply(to: Mailbox): To = To(List(to))
}
