package pencil
import data.*
import protocol.*
import ContentType.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalacheck.*
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.nio.file.{Path, Paths}

class ContentTypeFinderSpec extends Specification with ScalaCheck {
  import ContentTypeFinderSpec.*

  "ContentTypeFinder" should {

    "find content type" in forAll(pathGen) { case (p, t) =>
      findContentType(p).attempt.unsafeRunSync() === Right(t)
    }

    "not find file" in {
      val f = Paths.get("files/!!!jpeg-sample.jpg")
      Files
        .inputStream[IO](f)
        .use { is =>
          ContentTypeFinder
            .findType[IO](is)
        }
        .attempt
        .unsafeRunSync() must beLeft(Error.ResourceNotFound(f.toString))
    }
  }

}

object ContentTypeFinderSpec {
  def path(filename: String): Path =
    Paths.get(getClass.getClassLoader.getResource(filename).toURI)

  def findContentType(
      path: Path
  ): IO[ContentType] =
    Files
      .inputStream[IO](path)
      .use { is =>
        ContentTypeFinder
          .findType[IO](is)
      }

  val files = List(
    ("files/ascii-sample.txt", `text/plain`),
    ("files/html-sample.html", `text/html`),
    ("files/image-sample.png", `image/png`),
    ("files/gif-sample.gif", `image/gif`),
    ("files/jpeg-sample.jpg", `image/jpeg`),
    ("files/rfc2045.pdf", `application/pdf`)
  ).map { case (f, t) => (path(f), t) }

  val pathGen: Gen[(Path, ContentType)] = Gen.oneOf(files)
}
