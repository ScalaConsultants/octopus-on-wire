package data

import com.google.inject.Inject
import domain.{EventDao, EventFlagDao, EventJoinDao}
import tools.OffsetTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain._

class PersistentEventSource @Inject()(events: EventDao, eventJoins: EventJoinDao, eventFlags: EventFlagDao) extends EventSource {
  override def countPastJoinsBy(id: UserId): Future[Int] =
    events.countPastJoinsBy(id, OffsetTime.serverCurrent)

  override def countJoins(eventId: EventId): Future[Int] = eventJoins.countJoins(eventId)

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Future[Boolean] =
    eventJoins.userHasJoinedEvent(event, userId)

  override def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Future[Seq[Event]] =
    events.getEventsBetweenDatesNotFlaggedBy(from, to, userId)


  override def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Future[Seq[SimpleEvent]] =
    events.getFutureUnflaggedEvents(userId, limit, OffsetTime.serverCurrent)

  override def joinEvent(userId: UserId, eventId: EventId): Future[EventJoinMessage] =
    eventJoins.joinEvent(eventId, userId)

  override def getJoins(eventId: EventId): Future[Set[UserId]] = eventJoins.getJoiners(eventId)

  override def eventById(id: EventId): Future[Event] = events.findEventById(id)

  override def addEvent(event: Event): Future[EventAddition] =
    events.addEventAndGetId(event).map {
      case NoId => FailedToAdd("Unknown error")
      case _ => Added()
    }

  override def addFlag(eventId: EventId, by: UserId): Future[Boolean] =
    events.eventExists(eventId).flatMap {
      case true => eventFlags.flagEvent(eventId, by)
      case _ => Future.successful(false)
    }
}