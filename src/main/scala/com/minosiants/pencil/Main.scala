package com.minosiants.pencil

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import cats.implicits._
import cats.effect._
import fs2.Chunk
import fs2.Chunk.Bytes
import fs2.io.tcp.SocketGroup

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { socketGroup =>
          //client(socketGroup)
          client3(socketGroup, blocker)
        }
      }
      .as(ExitCode.Success)

  def toString(b: Array[Byte]) = {
    new String(b, StandardCharsets.US_ASCII)
  }

  def client(socketGroup: SocketGroup): IO[Unit] =
    socketGroup.client[IO](new InetSocketAddress("127.0.0.1", 25)).use {
      socket =>
        socket
          .read(8192)
          .flatMap {
            case Some(b @ Bytes(values, _, _)) =>
              IO(println(s"Response: ${toString(values)}"))
            case _ => IO(println(s"Response:"))

          }
          .attempt >> socket
          .write(
            Chunk.bytes("elho test <CRLF>".getBytes(StandardCharsets.US_ASCII))
          )
          .attempt >>
          socket.read(8192).flatMap {
            case Some(Bytes(values, _, _)) =>
              IO(println(s"Response: ${toString(values)}"))
            case _ => IO(println(s"Response:"))

          }
    }

  def client3(socketGroup: SocketGroup, blocker: Blocker): IO[Unit] =
    socketGroup.client[IO](new InetSocketAddress("127.0.0.1", 25)).use {
      socket =>
        for {
          v <- socket.read(8192).flatMap {
            case Some(b @ Bytes(values, _, _)) =>
              IO(println(s"Response: ${toString(values)}"))
            case _ => IO(println(s"Response:"))

          }
          w <- socket.write(
            Chunk.bytes("EHLO test \r\n".getBytes(StandardCharsets.US_ASCII))
          )
          r <- socket.read(8192).flatMap {
            case Some(Bytes(values, _, _)) =>
              IO(println(s"Response: ${toString(values)}"))
            case _ => IO(println(s"Response:"))

          }
        } yield ()
    }

}
