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
import cats.syntax.show.*

object FromType:

  opaque type From = Mailbox
  object From:

    def apply(box: Mailbox): From = box
    extension (self: From)
      def mailbox: Mailbox = self
      def address: String  = self.address

    given Show[From] = Mailbox.given_Show_Mailbox
