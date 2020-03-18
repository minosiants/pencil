package com.minosiants.pencil

import java.net.InetSocketAddress

import cats.effect.concurrent.{ Deferred, Ref }
import cats.effect.specs2.CatsIO
import cats.effect.{ Blocker, IO, Resource, Timer }
import com.minosiants.pencil.data.{ Email, Error }
import fs2.io.tcp.SocketGroup
import org.specs2.mutable.SpecificationLike
import scodec.{ Codec, DecodeResult }
import scodec.bits.BitVector
import cats.syntax.show._
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.either._

import scala.concurrent.duration._

trait SmtpBaseSpec extends SpecificationLike with CatsIO {

  def socket(
      address: InetSocketAddress,
      sg: SocketGroup
  ): Resource[IO, SmtpSocket] =
    SmtpSocket(address, 5.seconds, 5.seconds, sg)

  type ServerState = Ref[IO, List[BitVector]]

  def withSocket[A](run: (SmtpSocket, ServerState) => IO[A]): IO[A] = {
    val localBindAddress =
      Deferred[IO, InetSocketAddress].unsafeRunSync()

    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          for {
            state   <- Ref[IO].of(List.empty[BitVector])
            f       <- SmtpServer(sg, state).start(localBindAddress).start
            address <- localBindAddress.get
            r       <- socket(address, sg).use(s => run(s, state))
            _       <- f.cancel
          } yield r
        }
      }
  }

  def testCommand[A, B](
      command: Smtp[A],
      email: Email,
      codec: Codec[B]
  ): Either[Throwable, (A, List[B])] = {
    withSocket { (s, state) =>
      (for {
        _ <- Smtp.init()
        v <- command
        raw <- Smtp.liftF(Timer[IO].sleep(100.millis).flatMap { _ =>
          state.get
        })
        r <- Smtp.liftF(
          raw.traverse(
            bits =>
              codec.decode(bits).toEither match {
                case Right(DecodeResult(value, _)) => IO(value)
                case Left(err)                     => Error.smtpError(err.message)
              }
          )
        )
      } yield (v, r)).run(SmtpRequest(email, s))
    }.attempt.unsafeRunSync()

  }
}
