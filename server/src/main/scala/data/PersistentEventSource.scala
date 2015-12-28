package data

import java.util.Calendar

import domain.{EventJoinDao, EventDao, EventFlagDao}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalac.octopusonwire.shared.domain._

object PersistentEventSource extends EventSource {
  override def countPastJoinsBy(id: UserId): Long =
    waitFor(EventDao.countPastJoinsBy(id, currentUTC))

  override def countJoins(eventId: EventId): Long = waitFor(EventJoinDao.countJoins(eventId))

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean =
    waitFor(EventJoinDao.userHasJoinedEvent(event, userId))

  override def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Seq[Event] =
    waitFor(EventDao.getEventsBetweenDatesNotFlaggedBy(from, to, userId))

  def currentUTC = {
    val serverOffset = Calendar.getInstance.getTimeZone.getRawOffset
    System.currentTimeMillis - serverOffset
  }

  override def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Seq[SimpleEvent] =
    waitFor(EventDao.getFutureUnflaggedEvents(userId, limit, currentUTC))

  override def joinEvent(userId: UserId, eventId: EventId): EventJoinMessage = ??? //TODO

  override def getJoins(eventId: EventId): Set[UserId] = waitFor(EventJoinDao.getJoiners(eventId))

  override def eventById(id: EventId): Option[Event] = waitFor(EventDao.findEventById(id))

  override def addEvent(event: Event): EventAddition = {
    waitFor(EventDao.addEventAndGetId(event)) match {
      case NoId => FailedToAdd("Unknown error")
      case _ => Added()
    }
  }

  override def getFlaggers(eventId: EventId): Set[UserId] = waitFor(EventFlagDao.getFlaggers(eventId))

  override def addFlag(eventId: EventId, by: UserId): Boolean =
    waitFor(EventFlagDao.userHasFlaggedEvent(eventId, by).flatMap {
      case true => Future.successful(false)
      case _ => EventFlagDao.flagEvent(eventId, by)
    })

  override def countFlags(eventId: EventId): Long = waitFor(EventFlagDao.countFlags(eventId))

  //TODO make the calls asynchronous where possible
  def waitFor[T](f: Future[T]) = Await.result(f, Duration.Inf)
}
