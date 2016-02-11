package config

import scalac.octopusonwire.shared.config.SharedConfig.BackendDomain

object ServerConfig {
  val ReputationRequiredToAddEvents = 3 + 1 // we start with reputation equal to 1

  val MaxEventsInMonth = 30

  val Domain = s".$BackendDomain"
}
