package com.minosiants.pencil

import java.io.File
import java.nio.file.{ Path, Paths }

import cats.effect._
import cats.implicits._
import fs2.io.tcp.SocketGroup
import data._
import Client._
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
                println(value.show)
                ExitCode.Success
              case Left(error) =>
                error match {
                  case e: Error     => println(e.show)
                  case e: Throwable => e.printStackTrace()
                }
                ExitCode.Error
            }
        }
      }

  def ascii() = {
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
            "/Users/kaspar/stuff/sources/pencil/src/test/resources/files/gif-sample.gif"
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
          "/Users/kaspar/stuff/sources/pencil/src/test/resources/files/gif-sample.gif"
        )
      )
    )
  }
}
