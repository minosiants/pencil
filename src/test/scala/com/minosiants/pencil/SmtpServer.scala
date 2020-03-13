package com.minosiants.pencil

import java.net.InetSocketAddress

import cats.effect.concurrent.{ Deferred, Ref }
import cats.effect.{ ContextShift, IO }
import com.minosiants.pencil.protocol._
import fs2.Stream
import fs2.io.tcp.{ Socket, SocketGroup }
import scodec.bits.BitVector
import scodec.codecs._
import scodec.stream.{ StreamDecoder, StreamEncoder }
import scodec.{ Attempt, Codec, DecodeResult, Decoder }

final case class In(raw: BitVector, command: Command)

object In {
  lazy val codec: Codec[In] = Codec[In](
    (in: In) => Command.codec.encode(in.command),
    bits =>
      for {
        command <- Command.codec.decode(bits)
      } yield DecodeResult(In(bits, command.value), BitVector.empty)
  )
}

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
              s.read.through(CommandHandler(state)).through(s.writes)

          }
      }
      .compile
      .drain
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
    //.debug(r => r.toString, r => println(s">>> $r"))
      .through(StreamEncoder.many(Reply.repliesCodec).toPipeByte)
      .through(socket.writes())

  final val decoder = StreamDecoder.many(InDecoder.decoder)

}

object CommandHandler {

  def apply(
      state: Ref[IO, List[BitVector]]
  )(stream: Stream[IO, In]): Stream[IO, Replies] = {

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

      case In(raw, Text(Command.endEmail)) =>
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream(DataSamples.`250 OK`)

      case In(raw, Text(txt)) =>
        println(">>>>" + raw)
        Stream.eval_(state.tryUpdate(_ :+ raw)) ++
          Stream.empty
    }

  }

}

object InDecoder {
  val decoder = new Decoder[List[In]]() {
    val delimiter    = CRLF
    val ednEmailBits = Command.endEmail.toBitVector
    override def decode(bits: BitVector): Attempt[DecodeResult[List[In]]] = {

      def go(vec: BitVector): Attempt[List[In]] = {
        if (vec === ednEmailBits) {
          In.codec
            .decode(vec)
            .map { case DecodeResult(value, _) => List(value) }
        } else {
          val index = vec.indexOfSlice(delimiter)
          if (index < 0)
            Attempt.successful(List.empty[In])
          else {
            val (value, tail) = vec.splitAt(index)
            In.codec.decode(value ++ delimiter) match {
              case Attempt.Successful(DecodeResult(a, _)) =>
                go(tail.drop(delimiter.size)).map(v => a :: v)
              case Attempt.Failure(cause) => Attempt.Failure(cause)
            }
          }
        }
      }

      go(bits).map(DecodeResult(_, BitVector.empty))
    }
  }

}
