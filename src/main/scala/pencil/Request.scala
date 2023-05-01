package pencil

import java.time.Instant
import data.*
final case class Request[F[_]](
    email: Email,
    socket: SmtpSocket[F],
    host: Host,
    timestamp: Instant,
    uuid: () => String
)
