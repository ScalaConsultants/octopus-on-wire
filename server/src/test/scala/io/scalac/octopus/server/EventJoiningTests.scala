package io.scalac.octopus.server

import io.scalac.octopus.server.TestHelpers._

import scala.util.Try
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{AlreadyJoined, JoinSuccessful, TryingToJoinPastEvent}

class EventJoiningTests extends OctoSpec {
  "A user" must "not be able to join a past event" in {
    val api = new AuthorizedApiWithOldEvent
    (api joinEventAndGetJoins oldEvent.id).message.details shouldBe TryingToJoinPastEvent.toString
  }

  they should "still be able to join a future event" in {
    val api = new AuthorizedApiWithFutureEvent
    (api joinEventAndGetJoins sampleValidEvent.id).message.details shouldBe JoinSuccessful.toString
  }

  they should "not be able to join an event twice" in {
    val api = new AuthorizedApiWithFutureEvent
    (api joinEventAndGetJoins sampleValidEvent.id).message.details shouldBe JoinSuccessful.toString
    (api joinEventAndGetJoins sampleValidEvent.id).message.details shouldBe AlreadyJoined.toString
  }
}