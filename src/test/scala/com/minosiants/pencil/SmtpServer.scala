package com.minosiants.pencil

import cats.effect.{ Deferred, IO, Ref }
import com.comcast.ip4s.{ Host, IpLiteralSyntax, Port, SocketAddress }
import com.minosiants.pencil.protocol.Command._
import com.minosiants.pencil.protocol._
import fs2.Stream
import fs2.io.net.{ Network, Socket }
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scodec.bits.BitVector
import scodec.stream.{ StreamDecoder, StreamEncoder }

final case class SmtpServer(
    state: Ref[IO, List[BitVector]],
    port: Int = 5555
) {

  def start(
      localBindAddress: Deferred[IO, SocketAddress[Host]]
  ): IO[Unit] = {
    val logger = Slf4jLogger.getLogger[IO]
    println("before setup")
    Stream
      .resource(
        Network[IO].serverResource(Some(ip"127.0.0.1"), Port.fromInt(port))
      )
      .flatMap {
        case (localAddress, server) =>
          Stream.eval(localBindAddress.complete(localAddress)).drain ++
            server.flatMap {
              case socket =>
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
    socket.reads
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
