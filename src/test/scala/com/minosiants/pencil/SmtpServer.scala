package com.minosiants.pencil

import java.net.InetSocketAddress

import cats.effect.concurrent.{ Deferred, Ref }
import cats.effect.{ ContextShift, IO }
import com.minosiants.pencil.protocol._
import fs2.Stream
import fs2.io.tcp.{ Socket, SocketGroup }
import scodec.bits.BitVector
import scodec.stream.{ StreamDecoder, StreamEncoder }
import Command._

final case class SmtpServer(
    sg: SocketGroup,
    state: Ref[IO, List[BitVector]],
    port: Int = 5555
) {

  def start(
      localBindAddress: Deferred[IO, InetSocketAddress]
  )(implicit cs: ContextShift[IO]): IO[Unit] = {
    sg.serverWithLocalAddress[IO](new InetSocketAddress(5555))
      .flatMap {
        case Left(local) =>
          Stream.eval_(localBindAddress.complete(local))
        case Right(socketHandle) =>
          Stream.resource(socketHandle).flatMap { client =>
            val s = MessageSocket(client)
            s.write(DataSamples.`220 Greeting`) ++
              s.read.through(processCommand).through(s.writes)

          }
      }
      .compile
      .drain
  }

  def processCommand(stream: Stream[IO, In]): Stream[IO, Replies] = {

    stream.flatMap {
      case In(raw, Ehlo(_)) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.ehloReplies)

      case In(raw, Mail(_)) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Rcpt(_)) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Data) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`354 End data`)

      case In(raw, Quit) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`221 Buy`)

      case In(raw, Noop) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Rset) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Vrfy(_)) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Text(Command.endEmail)) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Text(_)) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream.empty
      case In(_, AuthLogin) =>
        ???
      case In(_, StartTls) =>
        ???
    }

  }
}

final case class MessageSocket(socket: Socket[IO])
    extends Product
    with Serializable {
  def read: Stream[IO, In] =
    socket
      .reads(1024)
      .through(decoder.toPipeByte[IO])
      .through { s =>
        s.flatMap(Stream.emits(_))
      }
  def write(replies: Replies): Stream[IO, Unit] =
    writes(Stream.emit(replies))

  def writes(stream: Stream[IO, Replies]): Stream[IO, Unit] =
    stream
      .through(StreamEncoder.many(Reply.repliesCodec).toPipeByte)
      .through(socket.writes())

  final val decoder: StreamDecoder[List[In]] =
    StreamDecoder.many(In.inListDecoder)

}
