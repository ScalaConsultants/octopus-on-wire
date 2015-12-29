package data

import java.util.Calendar

import domain.{EventDao, EventFlagDao, EventJoinDao}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain._

object PersistentEventSource extends EventSource {
  override def countPastJoinsBy(id: UserId): Future[Int] =
    EventDao.countPastJoinsBy(id, currentUTC)

  override def countJoins(eventId: EventId): Future[Int] = EventJoinDao.countJoins(eventId)

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Future[Boolean] =
    EventJoinDao.userHasJoinedEvent(event, userId)

  override def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Future[Seq[Event]] =
    EventDao.getEventsBetweenDatesNotFlaggedBy(from, to, userId)

  def currentUTC = {
    val serverOffset = Calendar.getInstance.getTimeZone.getRawOffset
    System.currentTimeMillis - serverOffset
  }

  override def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Future[Seq[SimpleEvent]] =
    EventDao.getFutureUnflaggedEvents(userId, limit, currentUTC)

  override def joinEvent(userId: UserId, eventId: EventId): Future[EventJoinMessage] =
    EventJoinDao.joinEvent(eventId, userId)

  override def getJoins(eventId: EventId): Future[Set[UserId]] = EventJoinDao.getJoiners(eventId)

  override def eventById(id: EventId): Future[Option[Event]] = EventDao.findEventById(id)

  override def addEvent(event: Event): Future[EventAddition] =
    EventDao.addEventAndGetId(event).map {
      case NoId => FailedToAdd("Unknown error")
      case _ => Added()
    }

  private def getFlaggers(eventId: EventId): Future[Set[UserId]] = EventFlagDao.getFlaggers(eventId)

  override def addFlag(eventId: EventId, by: UserId): Future[Boolean] =
    EventFlagDao.userHasFlaggedEvent(eventId, by).flatMap {
      case true => Future.successful(false)
      case _ => EventFlagDao.flagEvent(eventId, by)
    }

  override def countFlags(eventId: EventId): Future[Int] = EventFlagDao.countFlags(eventId)
}
