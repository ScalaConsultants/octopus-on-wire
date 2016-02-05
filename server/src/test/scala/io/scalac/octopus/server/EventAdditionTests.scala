package io.scalac.octopus.server

import io.scalac.octopus.server.TestHelpers._

import scalac.octopusonwire.shared.domain.FailedToAdd._
import scalac.octopusonwire.shared.domain._

class EventAdditionTests extends OctoSpec {
  "A user" must "not be able to add an event if they aren't logged in" in {
    new UnauthorizedApi addEvent sampleValidEvent shouldBe FailedToAdd(UserNotLoggedIn)
  }

  they must "not be able to add an event that ends in the past" in {
    new AuthorizedApi addEvent oldEvent shouldBe FailedToAdd(EventCantEndInThePast)
  }

  they should "not be able to add an event not having X past items joined" in{
    new AuthorizedApi addEvent sampleValidEvent shouldBe FailedToAdd(UserCantAddEventsYet)
  }

  they should "be able to add an event when logged in having X past events joined" in {
    new AuthorizedApiWithJoinedPastEvents addEvent sampleValidEvent shouldBe Added()
  }

}
