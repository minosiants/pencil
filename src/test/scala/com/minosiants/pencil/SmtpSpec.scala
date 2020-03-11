package com.minosiants.pencil
import java.net.InetSocketAddress

import cats.effect.specs2.CatsIO
import cats.effect.{ Blocker, IO, Resource }
import com.minosiants.pencil.protocol._
import data._
import fs2.io.tcp.{ Socket, SocketGroup }
import fs2.Stream
import org.specs2.mutable.Specification
import scodec.Codec
import scodec.bits._
import scodec.codecs._
import scodec.stream.{ StreamDecoder, StreamEncoder }

import scala.concurrent.duration._

class SmtpSpec extends Specification with CatsIO {

  def socket(sg: SocketGroup): Resource[IO, SmtpSocket] =
    SmtpSocket("localhost", 5555, 5.seconds, 5.seconds, sg)

  def withSocket[A](run: SmtpSocket => IO[A]) = {
    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          socket(sg).use(s => run(s))
        }
      }
  }

  "Smtp" should {
    val r = Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          IO(SmtpServer(sg))
        }
      }
      .attempt
      .unsafeRunSync()

    "get response on EHLO" in {
      val result = withSocket { s =>
        (for {
          i <- Smtp.init()
          v <- Smtp.ehlo()
        } yield List(i, v)).run(Request(SmtpSpec.ascii(), s))
      }.attempt.unsafeRunSync()
      println(result)
      success
    }
  }

}

object SmtpSpec {

  def ascii(): AsciiEmail = {
    Email.ascii(
      From(Mailbox.unsafeFromString("user1@mydomain.tld")),
      To(Mailbox.unsafeFromString("user1@example.com")),
      Subject("first email"),
      Body.Ascii("hello")
    )
  }

}
