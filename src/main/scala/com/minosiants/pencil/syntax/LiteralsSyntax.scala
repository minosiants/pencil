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
package syntax

import java.nio.file.Paths
import com.minosiants.pencil.data._

trait LiteralsSyntax {
  implicit def pencilLiteralsSyntax(sc: StringContext): LiteralsOps =
    new LiteralsOps(sc)
}

class LiteralsOps(val sc: StringContext) extends AnyVal {

  def mailbox(args: Any*): Mailbox =
    macro LiteralSyntaxMacros.mailboxInterpolator

  def to(args: Any*): To = macro LiteralSyntaxMacros.toInterpolator
  def from(args: Any*): From = macro LiteralSyntaxMacros.fromInterpolator
  def cc(args: Any*): Cc = macro LiteralSyntaxMacros.ccInterpolator
  def bcc(args: Any*): Bcc = macro LiteralSyntaxMacros.bccInterpolator
  def subject(): Subject = Subject(sc.s())
  def attachment(): Attachment = Attachment(Paths.get(sc.s()))

}
