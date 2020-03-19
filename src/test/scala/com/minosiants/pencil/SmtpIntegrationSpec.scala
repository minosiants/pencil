package com.minosiants.pencil

import java.net.InetSocketAddress

import cats.effect.{ Blocker, IO }
import cats.effect.specs2.CatsIO
import com.minosiants.pencil.data.{
  Body,
  Credentials,
  Email,
  Password,
  Username
}
import fs2.io.tcp.SocketGroup
import fs2.io.tls.TLSContext
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
    }
  }
}
