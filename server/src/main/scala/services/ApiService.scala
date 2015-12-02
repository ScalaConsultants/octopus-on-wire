package services
import tools.EventServerOps._

import config.ServerConfig
import config.ServerConfig.PastJoinsRequiredToAddEvents
import data.{EventSource, InMemoryEventSource}
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{Joined, EventNotFound, UserNotFound, TryingToJoinPastEvent}
import scalac.octopusonwire.shared.tools.LongRangeOps._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.FailedToAdd.{`The event can't end in the past`, `User not logged in`}
import scalac.octopusonwire.shared.domain._

class ApiService(tokenOpt: Option[String], userId: Option[UserId], eventSource: EventSource = InMemoryEventSource) extends Api {

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
        Joined
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

  override def addEvent(event: Event): EventAddition = userId match {
    case Some(_) if event isInTheFuture => eventSource.addEvent(event)
    case Some(_) => FailedToAdd(`The event can't end in the past`)
    case _ => FailedToAdd(`User not logged in`)
  }

  override def flagEvent(eventId: EventId): Unit =
    userId.foreach(eventSource.addFlag(eventId, _))


  override def canUserAddEvents(): Either[Boolean, Long] =
    userId.map {
      PastJoinsRequiredToAddEvents - eventSource.countPastJoinsBy(_)
    } match {
      case Some(joinsLeft) if joinsLeft == 0 => Left(true)
      case None => Left(false)
      case Some(joinsLeft) => Right(joinsLeft)
    }



  private def hasUserFlagged(event: Event) =
    userId.exists(eventSource.getFlaggers(event.id) contains)
}