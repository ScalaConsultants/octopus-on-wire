package services

import config.ServerConfig.PastJoinsRequiredToAddEvents
import data._
import domain.TrustedUsers
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
                 eventSource: EventSource, userCache: UserCache) extends Api {

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


  override def getUsersJoined(eventId: EventId, limit: Int): Set[UserInfo] = {
    val joinsFuture = eventSource.getJoins(eventId).flatMap { allEventJoins =>
      (tokenOpt zip userId match {
        //user is logged in
        case Seq((token, id)) => userCache.getOrFetchUserFriends(token, id).map { friends =>
          val (joinedFriends, otherJoins) = allEventJoins.filterNot(userId contains).partition(friends contains)

          //in case the user has more friends than limit
          val joinedFriendsLimited = joinedFriends take limit

          val othersLimited = otherJoins take (limit - joinedFriendsLimited.size)
          joinedFriendsLimited ++ othersLimited
        }
        // user not logged in
        case _ => Future.successful(allEventJoins take limit)
      }).flatMap { joins =>
        Future.sequence(joins.map(userCache.getOrFetchUserInfo(_, tokenOpt)))
          .map(_.flatten)
      }
    }

    Await.result(
      joinsFuture,
      atMost = timeout
    )
  }

  override def addEvent(event: Event): EventAddition = userId.map { uid =>
    val eventIsInFuture = event.isInTheFuture

    val canAddFuture = getUserReputationFuture(uid).map(_.canAddEvents)

    val result = canAddFuture.flatMap {
      case true if eventIsInFuture => eventSource.addEvent(event)
      case _ if !eventIsInFuture => Future.successful(FailedToAdd(EventCantEndInThePast))
      case _ => Future.successful(FailedToAdd(UserCantAddEventsYet))
    }

    Await.result(
      result,
      atMost = timeout
    )
  }.getOrElse(FailedToAdd(UserNotLoggedIn))

  override def flagEvent(eventId: EventId): Boolean =
    Await.result(
      userId.map(eventSource.addFlag(eventId, _)).getOrElse(Future.successful(false)),
      atMost = timeout
    )

  private def getUserReputationFuture(id: UserId): Future[UserReputationInfo] = {
    val isTrustedFuture = TrustedUsers.isUserTrusted(id)
    val canAddFuture = eventSource.countPastJoinsBy(id).map { rep =>
      UserReputationInfo(rep.toLong, PastJoinsRequiredToAddEvents)
    }
    (isTrustedFuture zip canAddFuture).map {
      case (true, _) => TrustedReputationInfo
      case (_, info) => info
    }
  }

  override def getUserReputation(): Option[UserReputationInfo] =
    userId.map(getUserReputationFuture).map { rep =>
      Await.result(rep, timeout)
    }
}