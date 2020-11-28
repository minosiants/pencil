package com.minosiants.pencil.data

import org.specs2.mutable.Specification

class HostSpec extends Specification{

  "Host" should {
    "get local hostname" in {
      val host = Host.local()
      host.name !== "unknown"
    }
  }
}
