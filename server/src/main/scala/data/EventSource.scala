package data

import scalac.octopusonwire.shared.domain._

trait EventSource {
  def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Seq[Event]

  def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Seq[SimpleEvent]

  def countPastJoinsBy(id: UserId): Long

  def joinEvent(userId: UserId, eventId: EventId): EventJoinMessage

  def eventById(id: EventId): Option[Event]

  def countJoins(eventId: EventId): Long

  def getJoins(eventId: EventId): Set[UserId]

  def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean

  def countFlags(eventId: EventId): Long

  def getFlaggers(eventId: EventId): Set[UserId]

  /**
    * @return false if the event was already flagged by user or doesn't exist
    *         //TODO "refactor to messages" opportunity?
    * */
  def addFlag(eventId: EventId, by: UserId): Boolean

  def addEvent(event: Event): EventAddition
}
