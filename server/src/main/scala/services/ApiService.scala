package services

import tools.EventServerOps._

import config.ServerConfig.PastJoinsRequiredToAddEvents
import data.{PersistentEventSource, EventSource, InMemoryEventSource}
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.FailedToAdd.{UserCantAddEventsYet, EventCantEndInThePast, UserNotLoggedIn}
import scalac.octopusonwire.shared.domain._

class ApiService(tokenOpt: Option[String], userId: Option[UserId], eventSource: EventSource = PersistentEventSource) extends Api {

  override def getUserEventInfo(eventId: EventId): Option[UserEventInfo] =
    eventSource.eventById(eventId) match {
      case Some(event) =>
        Option(UserEventInfo(
          event,
          userId exists (token => eventSource.hasUserJoinedEvent(eventId, token)),
          eventSource.countJoins(eventId),
          event isInTheFuture
        ))
      case None => None
    }

  override def getUserInfo(): Option[UserInfo] =
    userId.flatMap { id =>
      Await.result(
        awaitable = InMemoryUserCache.getOrFetchUserInfo(id, tokenOpt),
        atMost = Duration.Inf
      )
    }

  override def getFutureItems(limit: Int): Seq[SimpleEvent] =
    eventSource.getSimpleFutureEventsNotFlaggedByUser(userId, limit)

  override def getEventsForRange(from: Long, to: Long): Seq[Event] =
    eventSource.getEventsBetweenDatesNotFlaggedBy(from, to, userId)

  override def isUserLoggedIn() = userId.isDefined

  override def joinEventAndGetJoins(eventId: EventId): EventJoinInfo = {
    val event = eventSource.eventById(eventId)
    val message = userId match {
      case Some(id) if event.isDefined && event.exists(_ isInTheFuture) =>
        eventSource.joinEvent(id, eventId)
      case None => UserNotFound
      case _ if event.isDefined => TryingToJoinPastEvent
      case _ => EventNotFound
    }

    EventJoinInfo(eventSource.countJoins(eventId), EventJoinMessage(message.toString))
  }

  override def getUsersJoined(eventId: EventId, limit: Int): Set[UserInfo] =
    Await.result(
      awaitable =
        InMemoryUserCache.getOrFetchUserFriends(tokenOpt).flatMap { friends =>
          val (joinedFriends, otherJoins) = eventSource.getJoins(eventId)
            .filterNot(userId contains).partition(friends contains)

          val othersLimited = otherJoins take (limit - joinedFriends.size)

          Future.sequence((joinedFriends ++ othersLimited).map(InMemoryUserCache.getOrFetchUserInfo(_, tokenOpt)))
        },
      atMost = Duration.Inf
    ).flatten

  override def addEvent(event: Event): EventAddition = {
    val eventIsInFuture = event.isInTheFuture
    val canAdd = getUserReputation().exists { case UserReputationInfo(rep, treshold) => rep >= treshold }

    userId match {
      case Some(_) if eventIsInFuture && canAdd => eventSource.addEvent(event)
      case Some(_) if eventIsInFuture => FailedToAdd(UserCantAddEventsYet)
      case Some(_) => FailedToAdd(EventCantEndInThePast)
      case _ => FailedToAdd(UserNotLoggedIn)
    }
  }

  override def flagEvent(eventId: EventId): Boolean =
    userId.exists(eventSource.addFlag(eventId, _))

  override def getUserReputation(): Option[UserReputationInfo] =
    userId.map { id =>
      UserReputationInfo(eventSource.countPastJoinsBy(id), PastJoinsRequiredToAddEvents)
    }

  override def addFakeEvents(): Unit = {
    userId match {
      case Some(id) => InMemoryEventSource.addFakeUserJoins(id)
      case _ => ()
    }
  }
}