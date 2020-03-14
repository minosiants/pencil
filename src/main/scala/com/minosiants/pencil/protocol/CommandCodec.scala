package com.minosiants.pencil
package protocol

import com.minosiants.pencil.data.{ Error, Mailbox }
import scodec.bits.BitVector
import scodec.codecs._
import scodec.{ Attempt, Codec, DecodeResult, Err, SizeBound }

final case class CommandCodec() extends Codec[Command] {
  override def decode(bits: BitVector): Attempt[DecodeResult[Command]] = {
    limitedSizeBits(4 * 8, ascii).decode(bits).flatMap {
      case DecodeResult(cmd, rest) =>
        cmd match {
          case "EHLO" =>
            ascii.decode(stripEND(rest)).map {
              case DecodeResult(domain, _) =>
                DecodeResult(Ehlo(domain.trim), BitVector.empty)
            }
          case "MAIL" =>
            mailboxCodec.decode(rest).map {
              case DecodeResult(email, _) =>
                DecodeResult(Mail(email), BitVector.empty)
            }
          case "RCPT" =>
            mailboxCodec.decode(rest.drop(4 * 8)).map {
              case DecodeResult(email, _) =>
                DecodeResult(Rcpt(email), BitVector.empty)
            }
          case "DATA" => Attempt.successful(DecodeResult(Data, BitVector.empty))
          case "QUIT" => Attempt.successful(DecodeResult(Quit, BitVector.empty))
          case _ =>
            ascii.decode(bits).map {
              case DecodeResult(txt, _) =>
                DecodeResult(Text(txt), BitVector.empty)
            }
        }
    }

  }

  override def encode(v: Command): Attempt[BitVector] =
    Attempt.successful(Command.CommandShow.show(v).toBitVector)

  override def sizeBound: SizeBound = SizeBound.unknown

  private val END = Command.end.toBitVector
  private def stripEND(bits: BitVector) = {
    bits.take(bits.size - END.size)
  }

  private val `<` = byte.encode('<').getOrElse(BitVector.empty)
  private val `>` = byte.encode('>').getOrElse(BitVector.empty)

  private def extractEmail(bits: BitVector): BitVector = {

    val from = bits.indexOfSlice(`<`)

    val (v, tail)  = bits.splitAt(from)
    val _tail      = tail.drop(`<`.size)
    val to         = _tail.indexOfSlice(`>`)
    val (email, _) = _tail.splitAt(to)
    email
  }

  lazy val mailboxCodec: Codec[Mailbox] =
    Codec[Mailbox](
      (mb: Mailbox) =>
        Attempt.successful(Mailbox.mailboxShow.show(mb).toBitVector),
      bits =>
        ascii.decode(extractEmail(bits)).flatMap {
          case DecodeResult(box, remainder) =>
            Mailbox.fromString(box) match {
              case Right(mb) =>
                Attempt.successful(DecodeResult(mb, remainder))
              case Left(error) =>
                Attempt.failure(Err(Error.errorShow.show(error)))
            }
        }
    )
}
