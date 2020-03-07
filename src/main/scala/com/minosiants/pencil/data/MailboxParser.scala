package com.minosiants.pencil.data

import scala.util.matching.Regex
import cats.syntax.either._
import cats.syntax.foldable._
import cats.instances.list._
import cats.instances.either._
import scala.Function._
import scala.language.postfixOps

object MailboxParser {

  type LocalPart = String
  type Domain    = String

  val special =
    List('<', '>', '(', ')', '[', ']', '\\', ',', ';', ':', '@', '\"')
  val domainPattern: Regex = "^(?!-)[a-zA-Z1-9.-]+[^-]$" r

  private def verifyLocalPart(localPart: String): Either[Error, String] = {

    def notValid(ch: Char): Boolean = {
      ch < 0 || ch > 127 || special.contains(ch)
    }

    localPart.toList
      .foldM(List.empty[Char]) {

        case (Nil, x) if notValid(x) =>
          Left(
            Error
              .InvalidMailBox(s"local part '$localPart' has invalid char $x'")
          )

        case (_, x) if notValid(x) =>
          Left(
            Error
              .InvalidMailBox(s"local part '$localPart' has invalid char $x'")
          )

        case (acc, x) if acc.last == '.' && x == '.' =>
          Left(
            Error.InvalidMailBox(s"local part '$localPart' has double dots $x'")
          )

        case (acc, x) => Right(acc :+ x)

      }
      .map(_.mkString)
  }

  private def verifyDomain(domain: String): Either[Error, String] =
    Either.fromOption(
      domainPattern.findFirstMatchIn(domain).map(const(domain)),
      Error.InvalidMailBox(s"invalid domain '$domain'")
    )

  private def verifyLength(box: String): Either[Error, String] = {
    if (box.length > 255)
      Left(Error.InvalidMailBox(s"mailbox is too long '$box''"))
    else
      Right(box)
  }

  private def split(box: String): Either[Error, (LocalPart, Domain)] =
    box.split("@").toList match {
      case Nil      => Left(Error.InvalidMailBox(s" '$box' does not have '@'"))
      case _ :: Nil => Left(Error.InvalidMailBox(s" '$box' does not have '@'"))
      case lp :: d  => Right(lp, d.mkString)
    }

  def parse(mailbox: String): Either[Error, Mailbox] = {
    for {
      _     <- verifyLength(mailbox)
      parts <- split(mailbox)
      lp    <- verifyLocalPart(parts._1)
      dom   <- verifyDomain(parts._2)
    } yield Mailbox(lp, dom)
  }

}
