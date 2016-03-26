package io.scalac.octopus.server

import org.scalatest.time.{Hours, Span}

import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{Event, EventId, NoId}

object TestData {
  def getSampleValidEvent = {
    val start = System.currentTimeMillis() + 10.hours.toMillis
    val end = start + 5.hours.toMillis
    Event(NoId, "Some valid event", start, end, 0, "Somewhere", "http://example.com")
  }

  def oldEvent = {
    val start = System.currentTimeMillis() - 10.hours.toMillis
    val end = start + 5.seconds.toMillis
    Event(NoId, "Some invalid event", start, end, 0, "Hell", "http://microsoft.com")
  }
}
