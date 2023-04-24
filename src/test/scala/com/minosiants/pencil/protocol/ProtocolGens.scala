package pencil
package protocol
import Command._
import data.EmailGens
import org.scalacheck.Gen

trait ProtocolGens extends EmailGens {

  val codeGen: Gen[Code] = Gen.oneOf(Code.values)

  val replyGen: Gen[Reply] = for {
    code <- codeGen
    sep <- Gen.oneOf(List("-", " "))
    text <- Gen.asciiPrintableStr
  } yield Reply(code, sep, text)

  val repliesGen: Gen[Replies] = Gen.nonEmptyListOf(replyGen).map(Replies(_))

  val commandGen: Gen[Command] = for {
    box <- mailboxGen
    domain <- Gen.asciiPrintableStr
    command <- Gen.oneOf(
      List[Command](
        Ehlo(domain),
        Mail(box),
        Rcpt(box),
        Vrfy(domain),
        Data,
        Rset,
        Noop,
        Quit,
        Text(domain),
        AuthLogin,
        StartTls
      )
    )
  } yield command
}
