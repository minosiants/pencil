package pencil

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

trait MailServerContainer:
  def foo(): String
  def start(): Unit
  def stop(): Unit

object MailServerContainer:
  def mk() =
    val container = GenericContainer(DockerImageName.parse("mailserver/docker-mailserver"))
    container.addEnv("LOG_LEVEL", "debug")
    new MailServerContainer:
      override def start(): Unit = container.start()

      override def stop(): Unit = container.stop()

      override def foo(): String = ""
