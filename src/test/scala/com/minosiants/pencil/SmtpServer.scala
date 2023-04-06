package com.minosiants.pencil

import cats.effect.{Deferred, IO, Ref}
import com.comcast.ip4s._
import com.minosiants.pencil.protocol.Command._
import com.minosiants.pencil.protocol._
import fs2.Stream
import fs2.io.net.{Network, Socket}
import scodec.bits.BitVector
import fs2.interop.scodec.{StreamDecoder, StreamEncoder}
import scodec.Codec
final case class SmtpServer(
    state: Ref[IO, List[BitVector]],
    port: Int = 5555
) {

  def start(
      localBindAddress: Deferred[IO, SocketAddress[Host]]
  ): IO[Unit] = {
    Stream
      .resource(
        Network[IO].serverResource(Some(ip"127.0.0.1"), Port.fromInt(port))
      )
      .flatMap { case (localAddress, server) =>
        Stream.eval(localBindAddress.complete(localAddress)).drain ++
          server.flatMap { socket =>
            val s = MessageSocket(socket)
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
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.ehloReplies)

      case In(raw, Mail(_)) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Rcpt(_)) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Data) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`354 End data`)

      case In(raw, Quit) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`221 Buy`)

      case In(raw, Noop) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Rset) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Vrfy(_)) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Text(Command.endEmail)) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Text(_)) =>
        Stream.eval(state.tryUpdate(_ :+ raw)).drain ++
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
    socket.reads
      .through(decoder.toPipeByte[IO])
      .through { s =>
        s.flatMap(Stream.emits(_))
      }
  def write(replies: Replies): Stream[IO, Unit] =
    writes(Stream.emit(replies))

  def writes(stream: Stream[IO, Replies]): Stream[IO, Unit] =
    stream
      .through(StreamEncoder.many(summon[Codec[Replies]]).toPipeByte)
      .through(socket.writes)

  final val decoder: StreamDecoder[List[In]] =
    StreamDecoder.many(In.inListDecoder)

}
