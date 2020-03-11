package com.minosiants.pencil

import java.net.InetSocketAddress

import cats.effect.{ ContextShift, IO }
import com.minosiants.pencil.protocol._
import fs2.Stream
import fs2.io.tcp.{ Socket, SocketGroup }
import scodec.stream.{ StreamDecoder, StreamEncoder }

final case class SmtpServer(sg: SocketGroup, port: Int = 5555) {

  def start()(implicit cs: ContextShift[IO]): IO[Unit] = {
    sg.server[IO](new InetSocketAddress(5555)).map { clientResource =>
      Stream
        .resource(clientResource)
        .flatMap { client =>
          {
            val s = MessageSocket(client)
            s.write(
              Replies(
                List(
                  Reply(
                    Code.code(220).get,
                    " ",
                    "mail.example.com ESMTP Postfix"
                  )
                )
              )
            )

            s.read.through(v => CommandHandler(v)).through(s.writes)
          }
        }
        .parJoin(100)
        .compile
        .drain

    }
  }

}

final case class MessageSocket(socket: Socket[IO])
    extends Product
    with Serializable {
  def read: Stream[IO, Command] =
    socket
      .reads(1024)
      .through(StreamDecoder.many(Command.codec).toPipeByte[IO])
  def write(replies: Replies): Stream[IO, Unit] =
    writes(Stream[IO, Replies](replies))

  def writes(stream: Stream[IO, Replies]): Stream[IO, Unit] =
    stream
      .through(StreamEncoder.many(Reply.repliesCodec).toPipeByte)
      .through(socket.writes(None))

}

object CommandHandler {
  def apply(stream: Stream[IO, Command]): Stream[IO, Replies] = {
    stream.map {
      case Ehlo(_) =>
        Replies(
          List(
            Reply(Code.code(250).get, "-", "mail.example.com"),
            Reply(Code.code(250).get, "-", "PIPELINING"),
            Reply(Code.code(250).get, " ", "8BITMIME")
          )
        )
      case Mail(_) =>
        Replies(
          Reply(Code.code(250).get, " ", "2.1.0 Ok")
        )
      case Rcpt(_) =>
        Replies(
          Reply(Code.code(250).get, " ", "2.1.0 Ok")
        )
      case Data =>
        Replies(
          Reply(Code.code(354).get, " ", "End data with <CR><LF>.<CR><LF>")
        )

      case Quit =>
        Replies(
          Reply(Code.code(221).get, " ", "2.0.0 Bye")
        )

    }
  }
}
