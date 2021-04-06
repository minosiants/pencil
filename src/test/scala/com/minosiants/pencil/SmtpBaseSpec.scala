package com.minosiants.pencil

import java.net.InetSocketAddress
import java.time.{ Clock, Instant, ZoneId, ZoneOffset }
import java.util.UUID

import cats.effect.concurrent.{ Deferred, Ref }
import cats.effect.{ Blocker, IO, Resource, Timer }
import cats.effect.testing.specs2.CatsIO
import cats.instances.list._
import cats.syntax.traverse._
import com.minosiants.pencil.data.{ Email, Error, Host }
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.specs2.mutable.SpecificationLike
import scodec.bits.BitVector
import scodec.{ Codec, DecodeResult }

import scala.Function.const
import scala.concurrent.duration._

trait SmtpBaseSpec extends SpecificationLike with CatsIO {

  val logger    = Slf4jLogger.getLogger[IO]
  val timestamp = Instant.now()
  val clock     = Clock.fixed(timestamp, ZoneId.from(ZoneOffset.UTC))
  val host      = Host.local()
  val uuid      = UUID.randomUUID().toString

  def socket(
      address: InetSocketAddress,
      sg: SocketGroup
  ): Resource[IO, SmtpSocket[IO]] =
    sg.client[IO](address)
      .map(SmtpSocket.fromSocket(_, logger, 5.seconds, 5.seconds))

  type ServerState = Ref[IO, List[BitVector]]

  def withSocket[A](
      run: (SmtpSocket[IO], Blocker, ServerState) => IO[A]
  ): IO[A] = {
    val localBindAddress =
      Deferred[IO, InetSocketAddress].unsafeRunSync()

    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          for {
            state   <- Ref[IO].of(List.empty[BitVector])
            f       <- SmtpServer(sg, state).start(localBindAddress).start
            address <- localBindAddress.get
            r       <- socket(address, sg).use(s => run(s, blocker, state))
            _       <- f.cancel
          } yield r
        }
      }
  }

  def testCommand[A, B](
      command: Smtp[IO, A],
      email: Email,
      codec: Codec[B]
  ): Either[Throwable, (A, List[B])] = {
    withSocket { (s, blocker, state) =>
      (for {
        _ <- Smtp.init[IO]()
        v <- command
        raw <- Smtp.liftF(Timer[IO].sleep(100.millis).flatMap { _ =>
          state.get
        })
        r <- Smtp.liftF(
          raw.traverse(
            bits =>
              codec.decode(bits).toEither match {
                case Right(DecodeResult(value, _)) => IO(value)
                case Left(err)                     => Error.smtpError[IO, B](err.message)
              }
          )
        )
      } yield (v, r))
        .run(
          Request(
            email,
            s,
            blocker,
            Host.local(),
            Instant.now(clock),
            () => uuid
          )
        )
    }.attempt.unsafeRunSync()

  }
}
