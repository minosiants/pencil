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

import java.nio.file.Paths

import cats.effect._
import cats.implicits._
import com.minosiants.pencil.Client._
import com.minosiants.pencil.data._
import fs2.io.tcp.SocketGroup

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          val client = Client("127.0.0.1")(sg)
          client
            .send(utf8())
            .attempt
            .map {
              case Right(value) =>
                ExitCode.Success
              case Left(error) =>
                error match {
                  case e: Error     => println(e.show)
                  case e: Throwable => println(e.getMessage)
                }
                ExitCode.Error
            }
        }
      }

  def ascii(): AsciiEmail = {
    Email.ascii(
      From(Mailbox.unsafeFromString("user1@mydomain.tld")),
      To(Mailbox.unsafeFromString("user1@example.com")),
      Subject("first email"),
      Body.Ascii("hello")
    )
  }
  def utf8(): MimeEmail = {
    Email
      .mime(
        From(Mailbox.unsafeFromString("user1@mydomain.tld")),
        To(Mailbox.unsafeFromString("user1@example.com")),
        Subject("привет"),
        Body.Utf8("hi there")
      )
      .addAttachment(
        Attachment(
          Paths.get(
            "path/to/file"
          )
        )
      )
  }
  def html(): MimeEmail = {
    val email = Email.mime(
      From(Mailbox.unsafeFromString("user1@mydomain.tld")),
      To(Mailbox.unsafeFromString("user1@example.com")),
      Subject("привет"),
      Body.Html(
        """<!DOCTYPE html><html><body><h1>My First Heading</h1><p>My first paragraph.</p></body></html>"""
      )
    )
    email.addAttachment(
      Attachment(
        Paths.get(
          "/path/to/file"
        )
      )
    )
  }
}
