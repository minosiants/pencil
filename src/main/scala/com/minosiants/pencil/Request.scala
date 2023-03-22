package com.minosiants.pencil

import java.time.Instant

import com.minosiants.pencil.data.Email
import com.minosiants.pencil.data.HostType.Host
final case class Request[F[_]](
    email: Email,
    socket: SmtpSocket[F],
    host: Host,
    timestamp: Instant,
    uuid: () => String
)
