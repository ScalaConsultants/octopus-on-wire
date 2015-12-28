package data

import java.util.Calendar

import domain.{EventDao, EventFlagDao}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalac.octopusonwire.shared.domain._

object PersistentEventSource extends EventSource {
  override def countPastJoinsBy(id: UserId): Long = 3 //TODO

  override def countJoins(eventId: EventId): Long = 0 //TODO

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean = false //TODO

  override def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Seq[Event] =
    waitFor(EventDao.getEventsBetweenDatesNotFlaggedBy(from, to, userId))

  override def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Seq[SimpleEvent] = {
    val serverOffset = Calendar.getInstance.getTimeZone.getRawOffset
    val currentUTC = System.currentTimeMillis - serverOffset

    waitFor(EventDao.getFutureUnflaggedEvents(userId, limit, currentUTC))
  }

  override def joinEvent(userId: UserId, eventId: EventId): EventJoinMessage = ??? //TODO

  override def eventById(id: EventId): Option[Event] = waitFor(EventDao.findEventById(id))

  override def addEvent(event: Event): EventAddition = {
    waitFor(EventDao.addEventAndGetId(event)) match {
      case NoId => FailedToAdd("Unknown error")
      case _ => Added()
    }
  }

  override def getFlaggers(eventId: EventId): Set[UserId] = waitFor(EventFlagDao.getFlaggers(eventId))

  override def addFlag(eventId: EventId, by: UserId): Boolean =
    waitFor(EventFlagDao.userHasFlaggedEvent(eventId, by).flatMap{
      case true => Future.successful(false)
      case _ => EventFlagDao.flagEvent(eventId, by)
    })

  override def getJoins(eventId: EventId): Set[UserId] = Set.empty //TODO

  override def countFlags(eventId: EventId): Long = waitFor(EventFlagDao.countFlags(eventId))

  //TODO make the calls asynchronous where possible
  def waitFor[T](f: Future[T]) = Await.result(f, Duration.Inf)
}
