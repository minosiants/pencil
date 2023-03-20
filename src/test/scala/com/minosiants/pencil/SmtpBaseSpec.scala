package com.minosiants.pencil

import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.instances.list.*
import cats.syntax.traverse.*
import com.comcast.ip4s.{Host, SocketAddress}
import com.minosiants.pencil.data.{Email, Error, Host as PHost}
import com.minosiants.pencil.syntax.LiteralsSyntax
import fs2.io.net.Network
import org.specs2.mutable.SpecificationLike
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scodec.bits.BitVector
import scodec.{Codec, DecodeResult}

import java.time.{Clock, Instant, ZoneId, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration.*
trait SmtpBaseSpec extends SpecificationLike with LiteralsSyntax{

  val logger    = Slf4jLogger.getLogger[IO]
  val timestamp = Instant.now()
  val clock     = Clock.fixed(timestamp, ZoneId.from(ZoneOffset.UTC))
  val host      = PHost.local()
  val uuid      = UUID.randomUUID().toString

  def socket(
      address: SocketAddress[Host]
  ): Resource[IO, SmtpSocket[IO]] =
    Network[IO]
      .client(address)
      .map(SmtpSocket.fromSocket(_, logger))

  type ServerState = Ref[IO, List[BitVector]]

  def withSocket[A](
      run: (SmtpSocket[IO], ServerState) => IO[A]
  ): IO[A] = {
    val localBindAddress =
      Deferred[IO, SocketAddress[Host]].unsafeRunSync()

    Resource
      .unit[IO]
      .use { _ =>
        for {
          state   <- Ref[IO].of(List.empty[BitVector])
          f       <- SmtpServer(state).start(localBindAddress).start
          address <- localBindAddress.get
          r       <- socket(address).use(s => run(s, state))
          _       <- f.cancel
        } yield r
      }
  }

  def testCommand[A, B](
      command: Smtp[IO, A],
      email: Email,
      codec: Codec[B]
  ): Either[Throwable, (A, List[B])] = {
    withSocket { (s, state) =>
      (for {
        _ <- Smtp.init[IO]()
        v <- command
        raw <- Smtp.liftF(Temporal[IO].sleep(100.millis).flatMap { _ =>
          state.get
        })
        r <- Smtp.liftF(
          raw.traverse(bits =>
            codec.decode(bits).toEither match {
              case Right(DecodeResult(value, _)) => IO(value)
              case Left(err) => Error.smtpError[IO, B](err.message)
            }
          )
        )
      } yield (v, r))
        .run(
          Request(
            email,
            s,
            PHost.local(),
            Instant.now(clock),
            () => uuid
          )
        )
    }.attempt.unsafeRunSync()

  }
}
