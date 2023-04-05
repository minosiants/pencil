package com.minosiants.pencil

import cats.effect.unsafe.implicits.global
import cats.effect.IO
import com.minosiants.pencil.data.*
import com.minosiants.pencil.syntax.*
import fs2.io.net.Network
import org.specs2.execute.Pending
import org.specs2.mutable.SpecificationLike
import org.typelevel.log4cats.slf4j.Slf4jLogger

class SmtpIntegrationSpec extends SpecificationLike with LiteralsSyntax {
  val logger = Slf4jLogger.getLogger[IO]

  "Smtp integration" should {
    "send text email" in {

      val email = Email.mime(
        from"user1@mydomain.tld",
        to"user1@example.com",
        subject"привет",
        Body.Utf8("hi there"),
        List(attachment"files/jpeg-sample.jpg")
      )

      val sendEmail = for {
        tls <- Network[IO].tlsContext.system
        client = Client[IO]()(tls, logger)
        response <- client.send(email)
      } yield response

      sendEmail.attempt.unsafeRunSync() match {
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
