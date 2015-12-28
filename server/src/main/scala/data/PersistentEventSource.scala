package data

import java.util.Calendar

import domain.EventDao

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scalac.octopusonwire.shared.domain._
import scala.concurrent.ExecutionContext.Implicits.global

object PersistentEventSource extends EventSource {
  override def countPastJoinsBy(id: UserId): Long = 3 //TODO

  override def countJoins(eventId: EventId): Long = 0 //TODO

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean = false //TODO

  override def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Seq[Event] = Nil //TODO

  override def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Seq[SimpleEvent] = {
    val serverOffset = Calendar.getInstance.getTimeZone.getRawOffset
    val currentUTC = System.currentTimeMillis - serverOffset

    waitFor(EventDao.getFutureUnflaggedEvents(userId, limit, currentUTC))
  }

  override def joinEvent(userId: UserId, eventId: EventId): EventJoinMessage = ??? //TODO

  override def eventById(id: EventId): Option[Event] = waitFor {
    EventDao.findEventById(id)
  }

  override def addEvent(event: Event): EventAddition = {
    waitFor(EventDao.addEventAndGetId(event)) match {
      case NoId => FailedToAdd("Unknown error")
      case _ => Added()
    }
  }

  override def getFlaggers(eventId: EventId): Set[UserId] = Set.empty

  override def addFlag(eventId: EventId, by: UserId): Boolean =
    waitFor(EventDao.userHasFlaggedEvent(eventId, by).flatMap{
      case true => Future.successful(false)
      case _ => EventDao.flagEvent(eventId, by)
    })


  override def getJoins(eventId: EventId): Set[UserId] = Set.empty

  override def countFlags(eventId: EventId): Long = ??? //TODO

  //TODO make the calls asynchronous where possible
  def waitFor[T](f: Future[T]) = Await.result(f, Duration.Inf)
}
