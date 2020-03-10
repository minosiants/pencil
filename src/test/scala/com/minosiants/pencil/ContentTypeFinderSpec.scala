package com.minosiants.pencil
import protocol._
import data._
import java.io.File
import java.nio.file.Paths

import cats.effect.IO
import org.specs2.mutable.Specification
import scodec.bits.BitVector

class ContentTypeFinderSpec extends Specification {

  def find(filename: String, expected: ContentType) = {
    val f = Paths.get(filename)

    Files
      .is(f)
      .use { is =>
        ContentTypeFinder
          .findType(java.nio.file.Files.newInputStream(f))
      }
      .attempt
      .unsafeRunSync() must beRight(expected)

  }
  "ContentTypeFinder" should {

    "find ascii content type" in {
      find("/files/ascii-sample.txt", ContentType.`text/plain`)
    }

    "find html content type" in {
      find("/files/html-sample.html", ContentType.`text/html`)
    }

    "find png content type" in {
      find("/files/image-sample.png", ContentType.`image/png`)
    }

    "find gif content type" in {
      find("/files/gif-sample.gif", ContentType.`image/gif`)
    }

    "find jpg content type" in {
      find("/files/jpeg-sample.jpg", ContentType.`image/jpeg`)
    }

    "not find file" in {

      val f = Paths.get("/files/!!!jpeg-sample.jpg")

      Files
        .is(f)
        .use { is =>
          ContentTypeFinder
            .findType(is)
        }
        .attempt
        .unsafeRunSync() must beLeft(Error.ResourceNotFound(f.toString))
    }

    "bala" in {
      val p = Paths.get(
        "/Users/kaspar/stuff/sources/pencil/src/test/resources/files/gif-sample.gif"
      )
      val result = Files
        .is(p)
        .use { v =>
          IO {
            val r = BitVector.fromInputStream(v).toBase64

            println(r.grouped(70).toList)
          }
        }
        .attempt
        .unsafeRunSync()
      println(result)
      success
    }
  }

}
