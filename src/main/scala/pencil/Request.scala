package pencil

import java.time.Instant
import data.*

import java.util.UUID
final case class Request[F[_]](
    email: Email,
    socket: SmtpSocket[F],
    host: Host  = Host.local(),
    timestamp: Instant = Instant.now(),
    uuid: () => String = () => UUID.randomUUID().toString
)
