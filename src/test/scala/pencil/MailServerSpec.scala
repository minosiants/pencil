package pencil

import org.specs2.mutable.Specification
import org.specs2.specification.{AfterAll, BeforeAll}

class MailServerSpec extends Specification with BeforeAll with AfterAll:
  val container = MailServerContainer.mk()

  override def beforeAll(): Unit = container.start()

  override def afterAll(): Unit = container.stop()

  "container" should {
    "do the test" in {
      true == true
    }
  }
