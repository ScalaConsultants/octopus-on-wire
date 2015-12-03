package io.scalac.octopus.server

import io.scalac.octopus.server.TestHelpers._

import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{JoinSuccessful, TryingToJoinPastEvent}

class EventJoiningTests extends OctoSpec {
  "A user" must "not be able to join a past event" in {
    val api = authorizedApiWithOldEvent
    (api joinEventAndGetJoins oldEvent.id).message.details shouldBe TryingToJoinPastEvent.toString
  }

  they should "still be able to join a future event" in {
    val api = authorizedApiWithFutureEvent
    (api joinEventAndGetJoins oldEvent.id).message.details shouldBe JoinSuccessful.toString
  }
}