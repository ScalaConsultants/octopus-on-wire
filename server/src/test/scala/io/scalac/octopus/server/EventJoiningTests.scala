package io.scalac.octopus.server

import data.InMemoryEventSource
import org.scalatest.time.{Hours, Span}
import org.scalatest.{FlatSpec, ShouldMatchers}
import services.ApiService
import TestHelpers._

import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{`Joined`, `Trying to join past event`}
import scalac.octopusonwire.shared.domain.FailedToAdd._
import scalac.octopusonwire.shared.domain._

trait OctoSpec extends FlatSpec with ShouldMatchers

private[server] object TestHelpers {
  val authorizedApi = new ApiService(Some("token"), Some(UserId(1)))
  val authorizedApiWithOldEvent = new ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def getEvents: Seq[Event] = oldEvent :: Nil
  })
  val authorizedApiWithFutureEvent = new ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def getEvents: Seq[Event] = sampleValidEvent :: Nil
  })

  val unauthorizedApi = new ApiService(None, None)

  val sampleValidEvent = {
    val start = System.currentTimeMillis() + 10.hours.toMillis
    val end = start + Span(5, Hours).toMillis
    Event(EventId(0), "Some valid event", start, end, 0, "Somewhere", "http://example.com")
  }
  val oldEvent = {
    val start = System.currentTimeMillis() - 10.hours.toMillis
    val end = start + 5.seconds.toMillis
    Event(EventId(0), "Some invalid event", start, end, 0, "Hell", "http://microsoft.com")
  }
}

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

class EventAddTests extends OctoSpec {
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