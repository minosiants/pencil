/*
 * Copyright 2020 Kaspar Minosiants
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.minosiants.pencil.data

import scala.util.matching.Regex
import cats.syntax.either.*
import cats.syntax.foldable.*
import cats.instances.list.*
import cats.instances.either.*
import scala.Function.*
import scala.language.postfixOps

object MailboxParser {

  type LocalPart = String
  type Domain    = String

  val special: List[Char] =
    List('<', '>', '(', ')', '[', ']', '\\', ',', ';', ':', '@', '\"')
  val domainPattern: Regex = "^(?!-)[a-zA-Z0-9.-]+[^-]$" r

  def verifyLocalPart(localPart: String): Either[Error, String] = {

    def notValid(ch: Char): Boolean = {
      ch < 33 || ch > 127 || special.contains(ch)
    }

    localPart.toList
      .foldM(List.empty[Char]) {

        case (Nil, x) if notValid(x) =>
          Left(
            Error
              .InvalidMailBox(s"local part '$localPart' has invalid char $x'")
          )
        case (Nil, x) => Right(Nil :+ x)
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

  def verifyDomain(domain: String): Either[Error, String] =
    Either.fromOption(
      domainPattern.findFirstMatchIn(domain).map(const(domain)),
      Error.InvalidMailBox(s"invalid domain '$domain'")
    )

  private def verifyLength(box: String): Either[Error, String] =
    if box.isBlank() then
      Left(Error.InvalidMailBox(s"mailbox has empty value '$box''"))
    else if box.length > 255 then
      Left(Error.InvalidMailBox(s"mailbox is too long '$box''"))
    else Right(box)

  private def split(box: String): Either[Error, (LocalPart, Domain)] =
    box.split("@").toList match {
      case Nil      => Left(Error.InvalidMailBox(s" '$box' does not have '@'"))
      case _ :: Nil => Left(Error.InvalidMailBox(s" '$box' does not have '@'"))
      case lp :: _ if lp.isBlank =>
        Left(Error.InvalidMailBox(s" '$box' does not have local part"))
      case _ :: d if d.mkString.isBlank =>
        Left(Error.InvalidMailBox(s" '$box' does not have domain"))
      case lp :: d => Right((lp, d.mkString))
    }

  def parse(mailbox: String): Either[Error, Mailbox] =
    for
      _     <- verifyLength(mailbox)
      parts <- split(mailbox)
      lp    <- verifyLocalPart(parts._1)
      dom   <- verifyDomain(parts._2)
    yield Mailbox(lp, dom)

}
