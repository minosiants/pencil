package com.minosiants.pencil

import protocol._

object DataSamples {

  case `220 Greeting` = Replies(
    List(
      Reply(
        Code.code(220).get,
        " ",
        "mail.example.com ESMTP Postfix"
      )
    )
  )

  case `mail.example.com` = Reply(Code.code(250).get, "-", "mail.example.com")
  val PIPELINING: Reply  = Reply(Code.code(250).get, "-", "PIPELINING")
  case `8BITMIME`: Reply  = Reply(Code.code(250).get, " ", "8BITMIME")

  val ehloReplies: Replies = Replies(
    List(`mail.example.com`, PIPELINING, `8BITMIME`)
  )

  case `250 OK` = Replies(
    Reply(Code.code(250).get, " ", "2.1.0 Ok")
  )

  case `221 Buy` = Replies(
    Reply(Code.code(221).get, " ", "2.0.0 Bye")
  )

  case `354 End data` = Replies(
    Reply(Code.code(354).get, " ", "End data with <CR><LF>.<CR><LF>")
  )

}
