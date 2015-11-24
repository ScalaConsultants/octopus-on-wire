package io.scalac.octopus.server

import io.scalac.octopus.server.TestHelpers._

import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{`Joined`, `Trying to join past event`}

class EventJoiningTests extends OctoSpec {
  "A user" must "not be able to join a past event" in {
    val api = authorizedApiWithOldEvent
    (api joinEventAndGetJoins oldEvent.id).message.details shouldBe `Trying to join past event`.toString
  }

  they should "still be able to join a future event" in {
    val api = authorizedApiWithFutureEvent
    (api joinEventAndGetJoins oldEvent.id).message.details shouldBe `Joined`.toString
  }
}