package com.minosiants.pencil

import java.net.InetSocketAddress

import cats.effect.concurrent.{ Deferred, Ref }
import cats.effect.specs2.CatsIO
import cats.effect.{ Blocker, IO, Resource, Timer }
import cats.instances.list._
import cats.syntax.traverse._
import com.minosiants.pencil.data.{ Email, Error }
import fs2.io.tcp.SocketGroup
import org.specs2.mutable.SpecificationLike
import scodec.bits.BitVector
import scodec.{ Codec, DecodeResult }

import scala.concurrent.duration._

trait SmtpBaseSpec extends SpecificationLike with CatsIO {

  def socket(
      address: InetSocketAddress,
      sg: SocketGroup
  ): Resource[IO, SmtpSocket] =
    sg.client[IO](address).map(SmtpSocket.fromSocket(_, 5.seconds, 5.seconds))

  type ServerState = Ref[IO, List[BitVector]]

  def withSocket[A](run: (SmtpSocket, Blocker, ServerState) => IO[A]): IO[A] = {
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
      command: Smtp[A],
      email: Email,
      codec: Codec[B]
  ): Either[Throwable, (A, List[B])] = {
    withSocket { (s, blocker, state) =>
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
      } yield (v, r)).run(Request(email, s, blocker))
    }.attempt.unsafeRunSync()

  }
}
