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

package pencil
package data

object SubjectType:
  opaque type Subject <: Matchable = String

  object Subject:
    def apply(sub: String): Subject = sub
    def unapply(sub: Subject): Option[String & Matchable] = Some(sub)

  extension (self: Subject)
    def asString: String = self
    def toBase64 = self.asString.toBase64
