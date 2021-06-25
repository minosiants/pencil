package com.minosiants.pencil

import cats.effect.{Deferred, IO, Ref}
import com.comcast.ip4s.{Host, IpLiteralSyntax, SocketAddress}
import com.minosiants.pencil.protocol.Command._
import com.minosiants.pencil.protocol._
import fs2.Stream
import fs2.io.net.{Socket, SocketGroup}
import scodec.bits.BitVector
import scodec.stream.{StreamDecoder, StreamEncoder}

final case class SmtpServer(
    sg: SocketGroup[IO],
    state: Ref[IO, List[BitVector]],
    port: Int = 5555
) {

  def start(
      localBindAddress: Deferred[IO, SocketAddress[Host]]
  ): IO[Unit] = {
    val setup = for {
      serverSetup <- sg.serverResource(Some(host"localhost"), Some(port"25"))
      (localAddress, clients) = serverSetup
       ba = Stream(Left(localAddress)) ++ clients.map(Right(_))
    }yield ba.flatMap {
       case Left(localAddress) => Stream.eval_(localBindAddress.complete(localAddress))
       case Right(client) =>
           val s = MessageSocket(client)
           s.write(DataSamples.`220 Greeting`) ++
             s.read.through(processCommand).through(s.writes)

         }
      Stream.resource(setup).compile.drain

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
      .reads
      .through(decoder.toPipeByte[IO])
      .through { s =>
        s.flatMap(Stream.emits(_))
      }
  def write(replies: Replies): Stream[IO, Unit] =
    writes(Stream.emit(replies))

  def writes(stream: Stream[IO, Replies]): Stream[IO, Unit] =
    stream
      .through(StreamEncoder.many(Replies.codec).toPipeByte)
      .through(socket.writes)

  final val decoder: StreamDecoder[List[In]] =
    StreamDecoder.many(In.inListDecoder)

}
