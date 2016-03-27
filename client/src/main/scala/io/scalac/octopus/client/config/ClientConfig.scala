package io.scalac.octopus.client.config

import java.nio.ByteBuffer

import concurrent.duration._
import autowire.ClientProxy
import boopickle.Default

import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.config.SharedConfig.BackendDomain

object ClientConfig {
  val EmptyListPlaceholderText = "Launch an event now for maximum visibility!"

  val TwitterSharingText: String = "I just found about %s - join me there!"

  val UsersToDisplay = 10
  val UserThumbSize = 100
  val ItemsToFetch = 5
  val ItemChangeInterval = 5.seconds
  val MoveToCalendarDelay = 2.seconds
  val KeyCheckDelay = 50.milliseconds
  val InitialSlideIndex = 0
  val WindowLoadTime = 300.milliseconds
  val WindowOpenDelay = 50.milliseconds
  val WeeksToDisplay = 6
  val ApiUrl = s"http://$BackendDomain:9000"

  type ClientApi = ClientProxy[Api, ByteBuffer, Default.Pickler, Default.Pickler]
  val octoApi: ClientApi = AutowireClient[Api]
}