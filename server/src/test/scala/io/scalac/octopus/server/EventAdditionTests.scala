package io.scalac.octopus.server

import io.scalac.octopus.server.TestHelpers._

import scalac.octopusonwire.shared.domain.FailedToAdd._
import scalac.octopusonwire.shared.domain._

class EventAdditionTests extends OctoSpec {
  "A user" must "not be able to add an event if they aren't logged in" in {
    unauthorizedApi addEvent sampleValidEvent shouldBe FailedToAdd(`User not logged in`)
  }

  they must "not be able to add an event that ends in the past" in {
    authorizedApi addEvent oldEvent shouldBe FailedToAdd(`The event can't end in the past`)
  }

  they should "be able to add an event when logged in" in {
    authorizedApi addEvent sampleValidEvent shouldBe Added()
  }
}
