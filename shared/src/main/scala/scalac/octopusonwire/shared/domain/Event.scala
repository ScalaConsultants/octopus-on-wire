package scalac.octopusonwire.shared.domain

import scalac.octopusonwire.shared.config.SharedConfig.MillisecondsInHour
import scalac.octopusonwire.shared.domain.Event._
import scalac.octopusonwire.shared.tools.LongRangeOps.{int2IntRangeOps, long2IntRangeOps}

case class Event(id: EventId, name: String, startDate: Long, endDate: Long, offset: Long, url: String, location: String) {
  require(name.length inRange(3, 100), InvalidNameMessage)
  require(startDate < endDate, InvalidDatesMessage)
  require(offset inRange(-15 * MillisecondsInHour, 15 * MillisecondsInHour), InvalidOffsetMessage)
  require(location.length inRange(3, 100), InvalidLocationMessage)
  require(url.length inRange(3, 100), InvalidURLMessage)

  def toSimple: SimpleEvent = SimpleEvent(id, name)
}

object Event{
  val InvalidNameMessage = "The name should be between 3 and 100 characters in length"
  val InvalidDatesMessage = "The start date must be before end date"
  val InvalidOffsetMessage = "Offset is out of range (-15, 15) hours"
  val InvalidLocationMessage = "The location should be between 3 and 100 characters in length"
  val InvalidURLMessage = "The URL should be between 3 and 100 characters in length"
}

case class EventId(value: Long)

case class SimpleEvent(id: EventId, name: String)

case class UserEventInfo(event: Event, userJoined: Boolean, joinCount: Long, eventActive: Boolean)