package com.minosiants.pencil

import cats.effect.Blocker
import com.minosiants.pencil.data.Email

final case class Request[F[_]](email: Email, socket: SmtpSocket[F], blocker: Blocker)