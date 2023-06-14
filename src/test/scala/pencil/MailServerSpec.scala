package pencil

import data.*
import cats.effect.{Async, Concurrent, IO, Resource}
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s.{Port, SocketAddress, host, port}
import fs2.io.net.{Network, Socket}
import org.specs2.execute.Pending
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.{AfterAll, BeforeAll}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pencil.syntax.*
import cats.syntax.show.*
import cats.syntax.flatMap.*
import pencil.protocol.{Replies, Code}
import cats.syntax.either.*
class MailServerSpec extends SpecificationLike with LiteralsSyntax with BeforeAll with AfterAll:
  val logger = Slf4jLogger.getLogger[IO]
  val container = MailServerContainer.mk()

  override def beforeAll(): Unit = container.start()

  override def afterAll(): Unit = container.stop()

  def runC[R](command: Smtp[IO, R])(using email: Email): IO[R] =
    Network[IO].client(container.socketAddress()).use { s =>
      /*Smtp.rset[IO]() >>*/
      command.run(Request(email, SmtpSocket.fromSocket[IO](s, logger)))
    }

  def printError: PartialFunction[Throwable, Throwable] = {
    case e: pencil.data.Error =>
      println(s"error: ${e.show}")
      e
    case e: Throwable =>
      e.printStackTrace()
      e

  }
  extension [R](c: Smtp[IO, R])
    def runCommand(using email: Email): R = runC(c).unsafeRunSync()
    def attempt(using email: Email): Either[Throwable, R] =
      runC(c).attempt.unsafeRunSync().leftMap(printError)
