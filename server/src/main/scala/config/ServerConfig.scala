package config

import scalac.octopusonwire.shared.config.SharedConfig.BackendDomain

object ServerConfig {
  val DefaultReputation = 1

  val ReputationRequiredToAddEvents = 3 + DefaultReputation // we start with reputation equal to 1

  val MaxEventsInMonth = 30

  val CookieDomain = s".$BackendDomain"
}
