package com.minosiants.pencil.data

import java.net.InetAddress

import scala.util.Try
import cats.syntax.try_.*

object HostType:

  opaque type Host = String
  object Host:
    def local(): Host =
      Try(InetAddress.getLocalHost.getHostName).getOrElse("unknown")
    extension (self: Host) def name: String = self
