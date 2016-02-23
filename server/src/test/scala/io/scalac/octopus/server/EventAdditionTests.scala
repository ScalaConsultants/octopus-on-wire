package io.scalac.octopus.server

import io.scalac.octopus.server.TestHelpers._

import scala.language.postfixOps
import scalac.octopusonwire.shared.domain.FailedToAdd._
import scalac.octopusonwire.shared.domain._

class EventAdditionTests extends OctoSpec {
  "A user" must "not be able to add an event if they aren't logged in" in {
    whenReady(new UnauthorizedApi addEvent sampleValidEvent) {
      _ shouldBe FailedToAdd(UserNotLoggedIn)
    }
  }

  they must "not be able to add an event that ends in the past" in {
    whenReady(new AuthorizedApi addEvent oldEvent) {
      _ shouldBe FailedToAdd(EventCantEndInThePast)
    }
  }

  they should "not be able to add an event not having X past items joined" in {
    whenReady(new AuthorizedApi addEvent sampleValidEvent) {
      _ shouldBe FailedToAdd(UserCantAddEventsYet)
    }
  }

  they should "be able to add an event when logged in having X past events joined" in {
    whenReady(new AuthorizedApiWithJoinedPastEvents addEvent sampleValidEvent) {
      _ shouldBe Added()
    }
  }

}
