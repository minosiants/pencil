package com.minosiants.pencil

import com.minosiants.pencil.protocol.Command
import scodec.{Attempt, Codec, DecodeResult, Decoder}
import scodec.bits.{BitVector, ByteVector}

final case class In(raw: BitVector, command: Command)

object In {

  lazy val codec: Codec[In] = Codec[In](
    (in: In) => summon[Codec[Command]].encode(in.command),
    (bits: scodec.bits.BitVector) =>
      for {
        command <- summon[Codec[Command]].decode(bits)
      } yield DecodeResult(In(bits, command.value), BitVector.empty)
  )

  val inListDecoder: inListDecoder = new inListDecoder
  class inListDecoder extends Decoder[List[In]]() {
    val delimiter    = CRLF
    val endEmailBits = Command.endEmail.toByteVector
    override def decode(bits: BitVector): Attempt[DecodeResult[List[In]]] = {

      def go(vec: ByteVector): Attempt[List[In]] = {
        if vec === endEmailBits then
          In.codec
            .decode(vec.bits)
            .map { case DecodeResult(value, _) => List(value) }
        else
          val index = vec.indexOfSlice(delimiter)
          if index < 0 then Attempt.successful(List.empty[In])
          else
            val (value, tail) = vec.splitAt(index)
            In.codec.decode((value ++ delimiter).bits) match {
              case Attempt.Successful(DecodeResult(a, _)) =>
                go(tail.drop(delimiter.size)).map(v => a :: v)
              case Attempt.Failure(cause) => Attempt.Failure(cause)
            }
      }

      go(bits.toByteVector).map(DecodeResult(_, BitVector.empty))
    }
  }

}
