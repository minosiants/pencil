package com.minosiants.pencil
import com.minosiants.pencil.data.Mailbox

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
