package scalac.octopusonwire.shared.domain

import scalac.octopusonwire.shared.tools.IntRangeOps.int2IntRangeOps

case class Event(id: EventId, name: String, startDate: Long, endDate: Long, location: String, url: String) {
  require(startDate < endDate)
  require(name.length inRange(3, 100))
  require(location.length inRange(3, 100))
  require(url.length inRange(3, 100))

  def toSimple: SimpleEvent = SimpleEvent(id, name)
}

case class EventId(value: Long)

case class SimpleEvent(id: EventId, name: String)

case class UserEventInfo(event: Event, userJoined: Boolean, joinCount: Long)