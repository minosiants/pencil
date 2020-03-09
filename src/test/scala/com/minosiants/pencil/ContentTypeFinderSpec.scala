package com.minosiants.pencil
import protocol._
import data._

import java.io.File

import org.specs2.mutable.Specification

class ContentTypeFinderSpec extends Specification {

  def find(filename: String, expected: ContentType) = {
    val f = new File(filename)
    ContentTypeFinder
      .findType(f)
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

      val f = new File("/files/!!!jpeg-sample.jpg")
      ContentTypeFinder
        .findType(f)
        .attempt
        .unsafeRunSync() must beLeft(Error.ResourceNotFound(f.getAbsolutePath))
    }
  }

}
