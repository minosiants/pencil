package com.minosiants.pencil

import cats.effect.specs2.CatsIO
import cats.effect.{ Blocker, IO }
import com.minosiants.pencil.data._
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
import org.specs2.execute.Pending
import org.specs2.mutable.SpecificationLike

class SmtpIntegrationSpec extends SpecificationLike with CatsIO {
  "Smtp integration" should {
    "send text email" in {

      val result = Blocker[IO]
        .use { blocker =>
          SocketGroup[IO](blocker).use { sg =>
            TLSContext.system[IO](blocker).flatMap { tls =>
              val client = Client(
                "localhost",
                25,
                Some(
                  Credentials(
                    Username("user1@example.com"),
                    Password("12345678")
                  )
                ),
                tls
              )(sg)
              client.send(
                Email.text(
                  from"user1@example.com",
                  to"user1@example.com",
                  subject"hello",
                  Body.Ascii("hey there")
                )
              )
            }

          }
        }

      result.attempt.unsafeRunSync() match {
        case Right(value) =>
          println(value)
          IO(success)
        case Left(err) =>
          println(err)
          err.printStackTrace()
          IO(failure)
      }

      Pending("this is integration test")
    }
  }
}
