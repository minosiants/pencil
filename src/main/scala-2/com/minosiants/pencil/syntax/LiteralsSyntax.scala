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

package com.minosiants.pencil.syntax

import java.nio.file.Paths
import com.minosiants.pencil.data._
import org.typelevel.literally.Literally

trait LiteralsSyntax {
  implicit def pencilLiteralsSyntax(sc: StringContext): LiteralsOps =
    new LiteralsOps(sc)
}

class LiteralsOps(val sc: StringContext) extends AnyVal {

  def mailbox(args: Any*): Mailbox = macro LiteralsOps.MailboxLiteral.make
  def to(args: Any*): To = macro LiteralsOps.ToLiteral.make
  def from(args: Any*): From = macro LiteralsOps.FromLiteral.make
  def cc(args: Any*): Cc = macro LiteralsOps.CcLiteral.make
  def bcc(args: Any*): Bcc = macro LiteralsOps.BccLiteral.make
  def subject(): Subject       = Subject(sc.s())
  def attachment(): Attachment = Attachment(Paths.get(sc.s()))

}

object LiteralsOps {
  object MailboxLiteral extends Literally[Mailbox] {
    override def validate(
        c: ToLiteral.Context
    )(s: String): Either[String, c.Expr[Mailbox]] = {
      import c.universe._
      Mailbox
        .fromString(s)
        .left
        .map(_.getMessage)
        .map(_ => c.Expr(q"Mailbox.unsafeFromString($s)"))
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[Mailbox] =
      apply(c)(args: _*)
  }

  object ToLiteral extends Literally[To] {
    override def validate(
        c: ToLiteral.Context
    )(s: String): Either[String, c.Expr[To]] = {
      import c.universe._
      Mailbox
        .fromString(s)
        .left
        .map(_.getMessage)
        .map(_ => c.Expr(q"To(Mailbox.unsafeFromString($s))"))
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[To] = apply(c)(args: _*)
  }

  object FromLiteral extends Literally[From] {
    override def validate(
        c: FromLiteral.Context
    )(s: String): Either[String, c.Expr[From]] = {
      import c.universe._
      Mailbox
        .fromString(s)
        .left
        .map(_.getMessage)
        .map(_ => c.Expr(q"From(Mailbox.unsafeFromString($s))"))
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[From] = apply(c)(args: _*)
  }

  object CcLiteral extends Literally[Cc] {
    override def validate(
        c: FromLiteral.Context
    )(s: String): Either[String, c.Expr[Cc]] = {
      import c.universe._
      Mailbox
        .fromString(s)
        .left
        .map(_.getMessage)
        .map(_ => c.Expr(q"Cc(Mailbox.unsafeFromString($s))"))
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[Cc] = apply(c)(args: _*)
  }

  object BccLiteral extends Literally[Bcc] {
    override def validate(
        c: FromLiteral.Context
    )(s: String): Either[String, c.Expr[Bcc]] = {
      import c.universe._
      Mailbox
        .fromString(s)
        .left
        .map(_.getMessage)
        .map(_ => c.Expr(q"Bcc(Mailbox.unsafeFromString($s))"))
    }

    def make(c: Context)(args: c.Expr[Any]*): c.Expr[Bcc] = apply(c)(args: _*)
  }

}
