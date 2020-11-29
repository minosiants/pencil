package com.minosiants.pencil.data

import java.net.InetAddress

import scala.util.Try

case class Host(private[pencil] val name: String)

object Host {
  def local(): Host = Try(InetAddress.getLocalHost.getHostName).fold(
    _ => Host("unknown"),
    name => Host(name)
  )
}
