package com.minosiants.pencil

import java.net.InetSocketAddress

import cats.effect._
import cats.syntax.show._
import com.minosiants.pencil.protocol.Command._
import com.minosiants.pencil.protocol._
import fs2.Chunk
import fs2.Chunk.Bytes
import fs2.io.tcp.{ Socket, SocketGroup }
import scodec.bits.BitVector
import scodec.codecs._
import scodec.{ Attempt, DecodeResult }

import scala.concurrent.duration.FiniteDuration

trait SmtpSocket {

  def read(): IO[List[Reply]]
  def write(command: Command): IO[Unit]
}

object SmtpSocket {

  def bytesToReply(bytes: Array[Byte]): IO[List[Reply]] = {
    Reply.codecReplies.decode(BitVector(bytes)) match {
      case Attempt.Successful(DecodeResult(value, _)) => IO(value)
      case Attempt.Failure(cause)                     => Error.smtpError(cause.messageWithContext)
    }
  }

  def fromSocket(
      s: Socket[IO],
      readTimeout: FiniteDuration,
      writeTimeout: FiniteDuration
  ): SmtpSocket = new SmtpSocket {

    override def read(): IO[List[Reply]] =
      s.read(8192, Some(readTimeout)).flatMap {
        case Some(chunk) => bytesToReply(chunk.toArray)
        case None        => Error.smtpError("Nothing to read")
      }

    override def write(command: Command): IO[Unit] = {
      ascii.encode(command.show) match {
        case Attempt.Successful(value) =>
          s.write(Chunk.array(value.toByteArray), Some(writeTimeout))
        case Attempt.Failure(cause) => Error.smtpError(cause.messageWithContext)
      }

    }
  }

  def apply(
      host: String,
      port: Int,
      readTimeout: FiniteDuration,
      writeTimeout: FiniteDuration,
      sg: SocketGroup
  )(implicit cs: ContextShift[IO]): Resource[IO, SmtpSocket] = {
    sg.client[IO](new InetSocketAddress(host, port))
      .map(fromSocket(_, readTimeout, writeTimeout))
  }

}
