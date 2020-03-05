package com.minosiants.pencil

import cats.effect._
import cats.implicits._
import fs2.io.tcp.SocketGroup
import data._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          val client = Client("127.0.0.1")(sg)
          client.sendEmail(
            Email(
              From(Mailbox("kaspar@m.com")),
              To(List(Mailbox("user1@mydomain.tld"))),
              None,
              "hello"
            )
          ).attempt.map{
            case Right(value) =>
              println(value.show)
              ExitCode.Success
            case Left(error) =>
                  error match {
                    case e:Error => println(e.show)
                    case e:Throwable => e.printStackTrace()
                  }
              ExitCode.Error
          }
        }
      }

}