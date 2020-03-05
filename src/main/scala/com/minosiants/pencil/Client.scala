package com.minosiants.pencil

import cats.effect.{ ContextShift, IO, Resource }
import com.minosiants.pencil.data._
import com.minosiants.pencil.protocol._
import fs2.io.tcp.SocketGroup

import scala.concurrent.duration._

trait Client {
  def sendEmail(email: Email): IO[List[Replies]]
}

case class EmailClient(
    host: String,
    port: Int,
    readTimeout: FiniteDuration,
    writeTimeout: FiniteDuration,
    sg: SocketGroup
)(implicit cs: ContextShift[IO])
    extends Client {

  private lazy val socket: Resource[IO, SmtpSocket] =
    SmtpSocket(host, port, readTimeout, writeTimeout, sg)

  override def sendEmail(email: Email): IO[List[Replies]] = {
    socket.use { s =>
      val send = for {
        i <- Smtp.init()
        e <- Smtp.sendMail()
        q <- Smtp.quit()
      } yield q :: i :: e
      send.run(Request(email, s))

    }
  }
}

object Client {
  def apply(
      host: String,
      port: Int = 25,
      readTimeout: FiniteDuration = 5.minutes,
      writeTimeout: FiniteDuration = 5.minutes
  )(sg: SocketGroup)(implicit cs: ContextShift[IO]): Client =
    EmailClient(host, port, readTimeout, writeTimeout, sg)

}
