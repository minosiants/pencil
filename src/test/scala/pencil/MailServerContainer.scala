package pencil

import com.comcast.ip4s.{Port, SocketAddress, host}
import org.testcontainers.containers.{BindMode, GenericContainer}
import org.testcontainers.containers.wait.strategy.{LogMessageWaitStrategy, Wait}
import org.testcontainers.utility.DockerImageName
import pencil.data.Credentials
import cats.syntax.show.*
import sys.process.*
import scala.jdk.CollectionConverters.*
trait MailServerContainer:
  def foo(): String
  def start(): Unit
  def stop(): Unit
  def smtpPort:Int
  def socketAddress() = SocketAddress(host"localhost", Port.fromInt(smtpPort).get)

  def credentials:Credentials
object MailServerContainer:
  def mk() =
    val username = Username("pencil")
    val password = Password("pencil1234")
    val container = GenericContainer(DockerImageName.parse("juanluisbaptiste/postfix:latest"))
      container.addEnv("SMTP_SERVER","mail.pencil.com")
      container.addEnv("SMTP_USERNAME",username.show)
      container.addEnv("SMTP_PASSWORD", password.show)
      container.addEnv("SERVER_HOSTNAME", "mail.pencil.com")
      container.addExposedPorts(25,25)
     // container.addExposedPorts(587, 587)
      //container.withNetwork()
     //container.addEnv("smtp_user", "pencil:pencil1234")
   //  container.withClasspathResourceMapping("certs", "/etc/postfix/certs", BindMode.READ_ONLY)


    //container.waitingFor(Wait.forLogMessage("mail.pencil.com is up and running", 1))
    new MailServerContainer:
      override def start(): Unit =
        container.start()
        container.waitingFor(Wait.forLogMessage("postfix entered RUNNING state", 1))


      override def stop(): Unit = container.stop()

      override def foo(): String = ""

      override def credentials: Credentials = Credentials(username, password)

      override def smtpPort = container.getMappedPort(25)
