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

package pencil.data

import cats.syntax.option.*
import pencil.data.Body.{Alternative, Ascii, Html, Utf8}

import scala.Function.const

enum Body:
  case Ascii(value: String)
  case Html(value: String)
  case Utf8(value: String)
  case Alternative(bodies: List[Body])
  def body: Option[String] = fold(_.value.some, _.value.some, _.value.some, const(None))
  def ascii: Option[Ascii] = fold(_.some, const(None), const(None), const(None))
  def html: Option[Html] = fold(const(None), _.some, const(None), const(None))
  def utf8: Option[Utf8] = fold(const(None), const(None), _.some, const(None))
  def alternative: Option[Alternative] = fold(const(None), const(None), const(None), _.some)
  def isAlternative: Boolean = alternative.isDefined
  def fold[A](
      ascii: Ascii => A,
      html: Html => A,
      utf8: Utf8 => A,
      alternative: Alternative => A
  ): A = this match
    case a @ Ascii(_)       => ascii(a)
    case h @ Html(_)        => html(h)
    case u @ Utf8(_)        => utf8(u)
    case a @ Alternative(_) => alternative(a)
