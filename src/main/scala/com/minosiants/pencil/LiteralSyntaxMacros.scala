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

package com.minosiants.pencil
import com.minosiants.pencil.data._

import scala.reflect.macros.blackbox
import scala.reflect.macros.blackbox.Context

/** Thanks to http4s and ip4s for the singlePartInterpolator implementation
  * https://github.com/Comcast/ip4s/blob/b4f01a4637f2766a8e12668492a3814c478c6a03/shared/src/main/scala/com/comcast/ip4s/LiteralSyntaxMacros.scala
  */
object LiteralSyntaxMacros {

  def mailboxInterpolator(
      c: blackbox.Context
  )(args: c.Expr[Any]*): c.Expr[Mailbox] =
    singlePartInterpolator(c)(
      args,
      "Mailbox",
      Mailbox.fromString(_).isRight,
      s => c.universe.reify(Mailbox.unsafeFromString(s.splice))
    )

  def toInterpolator(
      c: blackbox.Context
  )(args: c.Expr[Any]*): c.Expr[To] =
    singlePartInterpolator(c)(
      args,
      "To",
      Mailbox.fromString(_).isRight,
      s => c.universe.reify(To(Mailbox.unsafeFromString(s.splice)))
    )
  def fromInterpolator(
      c: blackbox.Context
  )(args: c.Expr[Any]*): c.Expr[From] =
    singlePartInterpolator(c)(
      args,
      "From",
      Mailbox.fromString(_).isRight,
      s => c.universe.reify(From(Mailbox.unsafeFromString(s.splice)))
    )

  def ccInterpolator(
      c: blackbox.Context
  )(args: c.Expr[Any]*): c.Expr[Cc] =
    singlePartInterpolator(c)(
      args,
      "Cc",
      Mailbox.fromString(_).isRight,
      s => c.universe.reify(Cc(Mailbox.unsafeFromString(s.splice)))
    )

  def bccInterpolator(
      c: blackbox.Context
  )(args: c.Expr[Any]*): c.Expr[Bcc] =
    singlePartInterpolator(c)(
      args,
      "Bcc",
      Mailbox.fromString(_).isRight,
      s => c.universe.reify(Bcc(Mailbox.unsafeFromString(s.splice)))
    )

  private def singlePartInterpolator[A](c: Context)(
      args: Seq[c.Expr[Any]],
      typeName: String,
      validate: String => Boolean,
      construct: c.Expr[String] => c.Expr[A]
  ): c.Expr[A] = {
    import c.universe._
    identity(args)
    c.prefix.tree match {
      case Apply(
          _,
          List(Apply(_, (lcp @ Literal(Constant(p: String))) :: Nil))
          ) =>
        val valid = validate(p)
        if (valid) construct(c.Expr(lcp))
        else c.abort(c.enclosingPosition, s"invalid $typeName")
    }
  }
}
