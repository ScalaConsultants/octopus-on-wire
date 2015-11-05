package scalac.octopusonwire.shared.domain

case class Event(id: EventId, name: String, startDate: Long, endDate: Long, location: String, url: String) {
  def toSimple: SimpleEvent = SimpleEvent(id, name)
}

case class EventId(value: Long)

case class SimpleEvent(id: EventId, name: String)

case class UserEventInfo(event: Event, userJoined: Boolean, joinCount: Long)