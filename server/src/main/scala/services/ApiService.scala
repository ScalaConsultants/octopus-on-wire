package services

import tools.EventServerOps._

import config.ServerConfig
import config.ServerConfig.PastJoinsRequiredToAddEvents
import data.{EventSource, InMemoryEventSource}
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder._
import scalac.octopusonwire.shared.tools.LongRangeOps._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.FailedToAdd.{UserCantAddEventsYet, EventCantEndInThePast, UserNotLoggedIn}
import scalac.octopusonwire.shared.domain._

case class ApiService(tokenOpt: Option[String], userId: Option[UserId], eventSource: EventSource = InMemoryEventSource) extends Api {

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
        awaitable = UserCache.getOrFetchUserInfo(id, tokenOpt),
        atMost = Duration.Inf
      )
    }

  override def getFutureItems(limit: Int): Seq[SimpleEvent] = {
    val now = System.currentTimeMillis()
    eventSource.getEventsWhere { event =>
      !hasUserFlagged(event) &&
        (event.startDate >= now || event.endDate >= now)
    } sortBy (_.startDate) take limit map (_.toSimple)
  }

  override def getEventsForRange(from: Long, to: Long): Seq[Event] =
    eventSource.getEventsWhere { event =>
      !hasUserFlagged(event) &&
        (event.startDate inRange(from, to)) || (event.endDate inRange(from, to))
    } take ServerConfig.MaxEventsInMonth

  override def isUserLoggedIn() = userId.isDefined

  override def joinEventAndGetJoins(eventId: EventId): EventJoin = {
    val event = eventSource.eventById(eventId)
    val message = userId match {
      case Some(id) if event.isDefined && event.exists(_ isInTheFuture) =>
        eventSource.joinEvent(id, eventId)
      case None => UserNotFound
      case _ if event.isDefined => TryingToJoinPastEvent
      case _ => EventNotFound
    }

    EventJoin(eventSource.countJoins(eventId), EventJoinMessage(message.toString))
  }

  override def getUsersJoined(eventId: EventId, limit: Int): Set[UserInfo] =
    Await.result(
      awaitable =
        UserCache.getOrFetchUserFriends(tokenOpt).flatMap { friends =>
          val (joinedFriends, otherJoins) = eventSource.getJoins(eventId)
            .filterNot(userId contains).partition(friends contains)

          val othersLimited = otherJoins take (limit - joinedFriends.size)

          Future.sequence((joinedFriends ++ othersLimited).map(UserCache.getOrFetchUserInfo(_, tokenOpt)))
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

  private def hasUserFlagged(event: Event) =
    userId.exists(eventSource.getFlaggers(event.id) contains)
}