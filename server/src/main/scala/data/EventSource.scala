package data

import scalac.octopusonwire.shared.domain.{Event, EventId, UserId}

trait EventSource {
  def getEvents: Seq[Event]

  def getEventsWhere(filter: Event => Boolean): Seq[Event]

  def joinEvent(userId: UserId, eventId: EventId): Unit

  def eventById(id: EventId): Option[Event]

  def countJoins(eventId: EventId): Long

  def getJoins(eventId: EventId): Set[UserId]

  def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean
}
