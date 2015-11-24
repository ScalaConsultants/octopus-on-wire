package io.scalac.octopus.server

import scala.concurrent.duration._
import org.scalatest.time.{Hours, Span}
import org.scalatest.{FlatSpec, ShouldMatchers}
import services.ApiService

import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.FailedToAdd._
import scalac.octopusonwire.shared.domain._

class ConstraintTest extends FlatSpec with ShouldMatchers {
  lazy val sampleValidEvent = {
    val start = System.currentTimeMillis() + 10.hours.toMillis
    val end = start + Span(5, Hours).toMillis
    Event(EventId(0), "Some valid event", start, end, "Somewhere", "http://example.com")
  }
  lazy val oldEvent = {
    val start = System.currentTimeMillis() - 1.hour.toMillis
    val end = start + 5.seconds.toMillis
    Event(EventId(0), "Some invalid event", start, end, "Hell", "http://microsoft.com")
  }

  "A user" must "not be able to add an event if they aren't logged in" in {
    val api: Api = new ApiService(None, None)
    api addEvent sampleValidEvent shouldBe FailedToAdd(`User not logged in`)
  }

  they should "be able to add an event when logged in" in {
    val api: Api = new ApiService(Some("token"), Some(UserId(1)))
    api addEvent sampleValidEvent shouldBe Added()
  }

  they must "not be able to add an event that ends in the past" in {
    val api: Api = new ApiService(Some("token"), Some(UserId(1)))
    api addEvent oldEvent shouldBe FailedToAdd(`Event starts in the past`)
  }
}
