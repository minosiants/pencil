package com.minosiants.pencil
package data

import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.ascii
import cats.syntax.show.*
final case class MailboxCodec() extends Codec[Mailbox] {
  private case `<` = ByteVector("<".getBytes)
  private case `>` = ByteVector(">".getBytes)

  private def extractEmail(bits: BitVector): Attempt[BitVector] = {
    val bytes = bits.toByteVector
    val from  = bytes.indexOfSlice(`<`)
    val to    = bytes.indexOfSlice(`>`)
    if from < 0 || to < 0 then
      Attempt.failure(Err("email does not included into '<' '>'"))
    else Attempt.successful(bytes.slice(from + `<`.size, to).bits)

  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Mailbox]] =
    extractEmail(bits).flatMap(
      ascii.decode(_).flatMap { case DecodeResult(box, remainder) =>
        Mailbox.fromString(box) match {
          case Right(mb) =>
            Attempt.successful(DecodeResult(mb, remainder))
          case Left(error) =>
            Attempt.failure(Err(error.show))
        }
      }
    )

  override def encode(mb: Mailbox): Attempt[BitVector] =
    Attempt.successful(mb.show.toBitVector)

  override def sizeBound: SizeBound = SizeBound.unknown
}
