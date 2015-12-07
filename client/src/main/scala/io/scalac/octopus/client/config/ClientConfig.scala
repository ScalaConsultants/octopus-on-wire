package io.scalac.octopus.client.config

import java.nio.ByteBuffer

import autowire.ClientProxy
import boopickle.Default

import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.config.SharedConfig
import scalac.octopusonwire.shared.config.SharedConfig.BackendDomain

object ClientConfig {
  val EmptyListPlaceholderText = "There are no events for you right now :("

  val TwitterSharingText: String = "I just found about %s - join me there!"

  val UsersToDisplay = 10
  val UserThumbSize = 100
  val ItemsToFetch = 5
  val ItemChangeInterval = 5000
  val MoveToCalendarDelay = 2000
  val KeyCheckDelay = 50
  val InitialSlideIndex = 0
  val WindowLoadTime = 300
  val WindowOpenDelay = 50
  val WeeksToDisplay = 6
  val ApiUrl = s"http://$BackendDomain:9000"
  type ClientApi = ClientProxy[Api, ByteBuffer, Default.Pickler, Default.Pickler]
  val octoApi: ClientApi = AutowireClient[Api]
}