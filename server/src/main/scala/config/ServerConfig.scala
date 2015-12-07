package config

import scalac.octopusonwire.shared.config.SharedConfig.BackendDomain

object ServerConfig {
  val PastJoinsRequiredToAddEvents = 3

  val MaxEventsInMonth = 30

  val Domain = s".$BackendDomain"
}
