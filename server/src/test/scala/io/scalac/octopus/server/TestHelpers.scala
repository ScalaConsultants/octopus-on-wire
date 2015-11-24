package io.scalac.octopus.server

import data.InMemoryEventSource
import org.scalatest._
import org.scalatest.time.{Hours, Span}
import services.ApiService
import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{EventId, Event, UserId}

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

trait OctoSpec extends FlatSpec with ShouldMatchers