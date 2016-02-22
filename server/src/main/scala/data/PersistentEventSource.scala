package data

import java.util.Calendar

import domain.{Events, EventFlags, EventJoins}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain._

class PersistentEventSource extends EventSource {
  override def countPastJoinsBy(id: UserId): Future[Int] =
    Events.countPastJoinsBy(id, currentUTC)

  override def countJoins(eventId: EventId): Future[Int] = EventJoins.countJoins(eventId)

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Future[Boolean] =
    EventJoins.userHasJoinedEvent(event, userId)

  override def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Future[Seq[Event]] =
    Events.getEventsBetweenDatesNotFlaggedBy(from, to, userId)

  def currentUTC = {
    val serverOffset = Calendar.getInstance.getTimeZone.getRawOffset
    System.currentTimeMillis - serverOffset
  }

  override def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Future[Seq[SimpleEvent]] =
    Events.getFutureUnflaggedEvents(userId, limit, currentUTC)

  override def joinEvent(userId: UserId, eventId: EventId): Future[EventJoinMessage] =
    EventJoins.joinEvent(eventId, userId)

  override def getJoins(eventId: EventId): Future[Set[UserId]] = EventJoins.getJoiners(eventId)

  override def eventById(id: EventId): Future[Option[Event]] = Events.findEventById(id)

  override def addEvent(event: Event): Future[EventAddition] =
    Events.addEventAndGetId(event).map { 
      case NoId => FailedToAdd("Unknown error")
      case _ => Added()
    }

  private def getFlaggers(eventId: EventId): Future[Set[UserId]] = EventFlags.getFlaggers(eventId)

  override def addFlag(eventId: EventId, by: UserId): Future[Boolean] =
  Events.eventExists(eventId).flatMap{
    case true => EventFlags.flagEvent(eventId, by)
    case _ => Future.successful(false)
  }

}
