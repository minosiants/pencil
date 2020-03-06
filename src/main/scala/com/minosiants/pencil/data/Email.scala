package com.minosiants.pencil.data

final case class Mailbox(value: String)   extends Product with Serializable
final case class From(value: Mailbox)     extends Product with Serializable
final case class To(value: List[Mailbox]) extends Product with Serializable
final case class Cc(value: List[Mailbox]) extends Product with Serializable
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
