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

      val email = Email.mime(
        from"user1@mydomain.tld",
        to"user1@example.com",
        subject"привет",
        Body.Utf8("hi there")
      ) + attachment"files/jpeg-sample.jpg"

      val result = Blocker[IO]
        .use { blocker =>
          SocketGroup[IO](blocker).use { sg =>
            TLSContext.system[IO](blocker).flatMap { tls =>
              val client = Client()(blocker, sg, tls)
              client.send(email)
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
