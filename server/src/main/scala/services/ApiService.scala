package services

import config.ServerConfig
import data.{EventSource, InMemoryEventSource}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain._

class ApiService(userId: Option[UserId]) extends Api {

  val eventSource: EventSource = InMemoryEventSource

  override def getUserEventInfo(eventId: EventId): Option[UserEventInfo] =
    eventSource.eventById(eventId) match {
      case Some(event) =>
        Option(UserEventInfo(
          event,
          userId exists (token => eventSource.hasUserJoinedEvent(eventId, token)),
          eventSource.countJoins(eventId)
        ))
      case None => None
    }

  override def getUserInfo(): Option[UserInfo] =
    userId.flatMap { id =>
      Await.result(
        awaitable = UserCache.getOrFetchUserInfo(id),
        atMost = Duration.Inf
      )
    }

  override def getFutureItems(limit: Int): Seq[SimpleEvent] = {
    val now = System.currentTimeMillis()
    eventSource.getEventsWhere { event =>
      event.startDate >= now || event.endDate >= now
    } sortBy (_.startDate) take limit map (_.toSimple)
  }

  override def getEventsForRange(from: Long, to: Long): Seq[Event] =
    eventSource.getEventsWhere { event =>
      (event.startDate >= from && event.startDate <= to) ||
        (event.endDate >= from && event.endDate <= to)
    } take ServerConfig.MaxEventsInMonth

  override def isUserLoggedIn() = userId.isDefined

  override def joinEventAndGetJoins(eventId: EventId): Long = {
    userId match {
      case Some(id) =>
        eventSource.joinEvent(id, eventId)
      case None =>
        println("User not found")
    }
    eventSource.countJoins(eventId)
  }

  override def getUsersJoined(eventId: EventId, limit: Int): Set[UserInfo] =
    Await.result(
      awaitable =
        Future.sequence(
          eventSource.getJoins(eventId)
            .filterNot(userId.contains) take limit map UserCache.getOrFetchUserInfo
        ).map(_.flatten),
      atMost = Duration.Inf
    )
}