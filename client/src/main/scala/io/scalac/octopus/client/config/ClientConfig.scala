package io.scalac.octopus.client.config

import java.nio.ByteBuffer

import autowire.ClientProxy
import boopickle.Default

import scalac.octopusonwire.shared.Api

object ClientConfig {
  val ItemsToFetch = 5
  val ItemChangeInterval = 5000
  val InitialSlideIndex = 0
  val WindowLoadTime = 300
  val WindowOpenDelay = 50
  val WeeksToDisplay = 6
  val ApiUrl = "http://octowire.com:9000"
  type ClientApi = ClientProxy[Api, ByteBuffer, Default.Pickler, Default.Pickler]
  val octoApi: ClientApi = AutowireClient[Api]
}