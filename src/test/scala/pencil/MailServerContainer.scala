package pencil

import com.comcast.ip4s.{Port, SocketAddress, host}
import org.testcontainers.containers.{BindMode, GenericContainer}
import org.testcontainers.containers.wait.strategy.{LogMessageWaitStrategy, Wait}
import org.testcontainers.utility.DockerImageName
import pencil.data.Credentials
import cats.syntax.show.*

import java.nio.file.Paths
import sys.process.*
import scala.jdk.CollectionConverters.*
trait MailServerContainer:
  def start(): Unit
  def stop(): Unit
  def smtpPort:Int
  def httpPort:Int
  def socketAddress() = SocketAddress(host"localhost", Port.fromInt(smtpPort).get)

  def credentials:Credentials
object MailServerContainer:
  def mk() =
    val username = Username("pencil")
    val password = Password("pencil1234")
    val smtp = 1025
    val http = 8025

    val container = GenericContainer(DockerImageName.parse("axllent/mailpit"))
    container.withClasspathResourceMapping("certs", "/data", BindMode.READ_ONLY)
    container.addExposedPorts(smtp, http)
    //container.addExposedPorts(httpPort, httpPort)
    container.addEnv("MP_SMTP_AUTH_FILE","/data/pass.txt")
    container.addEnv("MP_SMTP_TLS_CERT","/data/certificate.crt")
    container.addEnv("MP_SMTP_TLS_KEY", "/data/keyfile.key")
    container.addEnv("MP_SMTP_AUTH_ALLOW_INSECURE", "true")

    new MailServerContainer:
      override def start(): Unit =
        container.start()
        container.waitingFor(Wait.forListeningPort())

      override def stop(): Unit = container.stop()

      override def credentials: Credentials = Credentials(username, password)

      override def httpPort: Int = container.getMappedPort(http)

      override def smtpPort: Int = container.getMappedPort(smtp)
