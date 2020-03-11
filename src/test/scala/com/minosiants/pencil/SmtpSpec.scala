package com.minosiants.pencil
import java.net.InetSocketAddress

import cats.effect.specs2.CatsIO
import cats.effect.{ Blocker, IO, Resource }
import com.minosiants.pencil.protocol._
import data._
import fs2.io.tcp.SocketGroup
import fs2.Stream
import org.specs2.mutable.Specification
import scodec.Codec
import scodec.bits._
import scodec.codecs._

import scala.concurrent.duration._

class SmtpSpec extends Specification with CatsIO {

  def socket(sg: SocketGroup): Resource[IO, SmtpSocket] =
    SmtpSocket("localhost", 2222, 5.seconds, 5.seconds, sg)

  def withSocket[A](run: SmtpSocket => IO[A]) = {
    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          socket(sg).use(s => run(s))
        }
      }
  }

  /*def smtpServer(sg: SocketGroup): IO[Unit] =
      sg.server(new InetSocketAddress(5555)).map { clientResource =>
        Stream.resource(clientResource).flatMap { client =>

          client.write(ascii.encode("220 mail.example.com ESMTP Postfix"))
          Stream.
          client.reads(8192)
            .through(text.utf8Decode)
            .through(client.writes())
        }
      }.parJoin(100).compile.drain


  }*/
  "Smtp" should {
    "read ok" in {
      /*
        val result = withSocket{s =>
            Smtp.read().run(Request(SmtpSpec.ascii(), s))
        }.attempt.unsafeRunSync()

      result must beRight(Reply(Code.code(220).get, " ", "text"))*/

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
