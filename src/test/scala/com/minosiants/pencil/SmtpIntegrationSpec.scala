package com.minosiants.pencil

import cats.effect.unsafe.implicits.global
import cats.effect.{ IO, Resource }
import com.minosiants.pencil.data._
import fs2.io.net.Network
import org.specs2.execute.Pending
import org.specs2.mutable.SpecificationLike
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SmtpIntegrationSpec extends SpecificationLike {
  val logger = Slf4jLogger.getLogger[IO]

  "Smtp integration" should {
    "send text email" in {

      val email = Email.mime(
        from"user1@mydomain.tld",
        to"user1@example.com",
        subject"привет",
        Body.Utf8("hi there")
      ) + attachment"files/jpeg-sample.jpg"

      Network[IO].tlsContext.system.flatMap { tls =>
              val client = Client[IO]()(tls, logger)
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
