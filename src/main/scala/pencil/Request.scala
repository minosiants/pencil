package pencil

import java.time.Instant

import pencil.data.Email
import pencil.data.HostType.Host
final case class Request[F[_]](
    email: Email,
    socket: SmtpSocket[F],
    host: Host,
    timestamp: Instant,
    uuid: () => String
)
