package services

import config.ServerConfig.PastJoinsRequiredToAddEvents
import data._
import tools.EventServerOps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder._
import scalac.octopusonwire.shared.domain.FailedToAdd.{EventCantEndInThePast, UserCantAddEventsYet, UserNotLoggedIn}
import scalac.octopusonwire.shared.domain._

class ApiService(tokenOpt: Option[String], userId: Option[UserId],
                 eventSource: EventSource = InMemoryEventSource,
                 userCache: UserCache = PersistentUserCache) extends Api {

  val timeout = 10.seconds

  override def getUserEventInfo(eventId: EventId): Option[UserEventInfo] = {
    val eventFuture = eventSource.eventById(eventId)
    val userJoinedFuture = userId.map(eventSource.hasUserJoinedEvent(eventId, _)).getOrElse(Future.successful(false))
    val joinCountFuture = eventSource.countJoins(eventId)

    val resultFuture = (for {
      Some(event) <- eventFuture
      joinResult <- userJoinedFuture
      joinCount <- joinCountFuture
    } yield {
      Option(UserEventInfo(
        event,
        joinResult,
        joinCount,
        event isInTheFuture
      ))
    }).fallbackTo(Future.successful(None))

    Await.result(resultFuture, timeout)
  }

  override def getUserInfo(): Option[UserInfo] =
    userId.flatMap { id =>
      Await.result(
        awaitable = userCache.getOrFetchUserInfo(id, tokenOpt),
        atMost = timeout
      )
    }

  override def getFutureItems(limit: Int): Seq[SimpleEvent] =
    Await.result(eventSource.getSimpleFutureEventsNotFlaggedByUser(userId, limit), timeout)

  override def getEventsForRange(from: Long, to: Long): Seq[Event] =
    Await.result(eventSource.getEventsBetweenDatesNotFlaggedBy(from, to, userId), timeout)

  override def joinEventAndGetJoins(eventId: EventId): EventJoinInfo = {
    val messageFuture = eventSource.eventById(eventId).flatMap { event =>
      userId match {
        case Some(id) if event.isDefined && event.exists(_ isInTheFuture) =>
          eventSource.joinEvent(id, eventId)
        case None => Future.successful(UserNotFound.apply)
        case _ if event.isDefined => Future.successful(TryingToJoinPastEvent.apply)
        case _ => Future.successful(EventNotFound.apply)
      }
    }

    Await.result(
      messageFuture.flatMap { ejm =>
        eventSource.countJoins(eventId).map(EventJoinInfo(_, ejm))
      }, timeout
    )
  }

  override def getUsersJoined(eventId: EventId, limit: Int): Set[UserInfo] =
    Await.result(
      userCache.getOrFetchUserFriends(tokenOpt).flatMap { friends =>
        val joinsFuture = eventSource.getJoins(eventId).map(_.filterNot(userId contains).partition(friends contains))
        joinsFuture.flatMap { case (joinedFriends, otherJoins) =>
          val othersLimited = otherJoins take (limit - joinedFriends.size)
          Future.sequence((joinedFriends ++ othersLimited).map(userCache.getOrFetchUserInfo(_, tokenOpt))).map(_.flatten)
        }
      },
      timeout
    )

  override def addEvent(event: Event): EventAddition = {
    val eventIsInFuture = event.isInTheFuture
    val canAdd = getUserReputation().exists { case UserReputationInfo(rep, treshold) => rep >= treshold }

    Await.result(
      userId match {
        case Some(_) if eventIsInFuture && canAdd => eventSource.addEvent(event)
        case Some(_) if eventIsInFuture => Future.successful(FailedToAdd(UserCantAddEventsYet))
        case Some(_) => Future.successful(FailedToAdd(EventCantEndInThePast))
        case _ => Future.successful(FailedToAdd(UserNotLoggedIn))
      },
      timeout
    )
  }

  override def flagEvent(eventId: EventId): Boolean =
    Await.result(
      userId.map(eventSource.addFlag(eventId, _)).getOrElse(Future.successful(false)),
      atMost = timeout
    )

  override def getUserReputation(): Option[UserReputationInfo] =
    userId.map { id =>
      Await.result(
        eventSource.countPastJoinsBy(id)
          .map { rep =>
            UserReputationInfo(rep.toLong, PastJoinsRequiredToAddEvents)
          },
        timeout
      )
    }
}