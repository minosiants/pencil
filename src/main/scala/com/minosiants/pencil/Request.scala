package com.minosiants.pencil

import cats.effect.Blocker
import com.minosiants.pencil.data.Email

final case class Request(email: Email, socket: SmtpSocket, blocker: Blocker)