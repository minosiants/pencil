package com.minosiants.pencil

import java.time.Instant

import cats.effect.Blocker
import com.minosiants.pencil.data.{ Email, Host }

final case class Request[F[_]](
    email: Email,
    socket: SmtpSocket[F],
    blocker: Blocker,
    host: Host,
    timestamp: Instant
)
