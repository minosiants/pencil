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
  extension (inline sc: StringContext)
    inline def mailbox(args: Any*): Mailbox = ${
      LiteralsOps.MailboxLiteral('sc, 'args)
    }
    inline def to(args: Any*): To     = ${ LiteralsOps.ToLiteral('sc, 'args) }
    inline def from(args: Any*): From = ${ LiteralsOps.FromLiteral('sc, 'args) }
    inline def cc(args: Any*): Cc     = ${ LiteralsOps.CcLiteral('sc, 'args) }
    inline def bcc(args: Any*): Bcc   = ${ LiteralsOps.BccLiteral('sc, 'args) }
    inline def subject(): Subject     = Subject(sc.s())
    inline def attachment(): Attachment = Attachment(Paths.get(sc.s()))
}

object LiteralsOps {
  object MailboxLiteral extends Literally[Mailbox] {
    def validate(s: String)(using Quotes) =
      Mailbox.fromString(s) match
        case Left(value) => Left(value.getMessage)
        case Right(_)    => Right('{ Mailbox.unsafeFromString(${ Expr(s) }) })
  }

  object ToLiteral extends Literally[To] {
    def validate(s: String)(using Quotes) =
      Mailbox.fromString(s) match
        case Left(value) => Left(value.getMessage)
        case Right(_) => Right('{ To(Mailbox.unsafeFromString(${ Expr(s) })) })
  }

  object FromLiteral extends Literally[From] {
    def validate(s: String)(using Quotes) =
      Mailbox.fromString(s) match
        case Left(value) => Left(value.getMessage)
        case Right(_) =>
          Right('{ From(Mailbox.unsafeFromString(${ Expr(s) })) })
  }

  object CcLiteral extends Literally[Cc] {
    def validate(s: String)(using Quotes) =
      Mailbox.fromString(s) match
        case Left(value) => Left(value.getMessage)
        case Right(_) => Right('{ Cc(Mailbox.unsafeFromString(${ Expr(s) })) })
  }

  object BccLiteral extends Literally[Bcc] {
    def validate(s: String)(using Quotes) =
      Mailbox.fromString(s) match
        case Left(value) => Left(value.getMessage)
        case Right(_) => Right('{ Bcc(Mailbox.unsafeFromString(${ Expr(s) })) })
  }

}
