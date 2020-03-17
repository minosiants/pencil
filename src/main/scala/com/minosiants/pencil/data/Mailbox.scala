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
package data

import cats.Show

final private[pencil] case class Mailbox(localPart: String, domain: String)
    extends Product
    with Serializable {

  def address: String = s"$localPart@$domain"
}

object Mailbox {

  def fromString(mailbox: String): Either[Error, Mailbox] =
    MailboxParser.parse(mailbox)
  def unsafeFromString(mailbox: String): Mailbox =
    fromString(mailbox).fold(throw _, identity)

  implicit val mailboxShow: Show[Mailbox] =
    Show.show[Mailbox](mb => s"<${mb.address}>")

}
