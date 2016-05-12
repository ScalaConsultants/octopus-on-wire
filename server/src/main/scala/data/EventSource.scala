package data

import scala.concurrent.Future
import scalac.octopusonwire.shared.domain._

trait EventSource {
  def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Future[Seq[Event]]

  def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Future[Seq[SimpleEvent]]

  def countPastJoinsBy(id: UserId): Future[Int]

  def joinEvent(userId: UserId, eventId: EventId): Future[EventJoinMessage]

  def eventById(id: EventId): Future[Option[Event]]

  def countJoins(eventId: EventId): Future[Int]

  def getJoins(eventId: EventId): Future[Set[UserId]]

  def hasUserJoinedEvent(event: EventId, userId: UserId): Future[Boolean]

  /**
    * @return false if the event was already flagged by user or doesn't exist
    *         //TODO "refactor to messages" opportunity?
    * */
  def addFlag(eventId: EventId, by: UserId): Future[Boolean]

  def addEvent(event: Event): Future[EventAddition]
}
