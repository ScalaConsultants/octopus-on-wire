package services

import config.ServerConfig._
import data._
import domain.UserIdentity
import tools.EventServerOps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder._
import scalac.octopusonwire.shared.domain.FailedToAdd.{EventCantEndInThePast, UserCantAddEventsYet, UserNotLoggedIn}
import scalac.octopusonwire.shared.domain._

class ApiService(userIdentity: Option[UserIdentity],
                 eventSource: EventSource, userCache: UserCache) extends Api {

  val timeout = 10.seconds

  override def getUserEventInfo(eventId: EventId): Option[UserEventInfo] = {
    val eventFuture = eventSource.eventById(eventId)
    val userJoinedFuture = userIdentity.map(_.id).map(eventSource.hasUserJoinedEvent(eventId, _)).getOrElse(Future.successful(false))
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
    userIdentity.flatMap { case UserIdentity(token, id) =>
      Await.result(
        awaitable = userCache.getOrFetchUserInfo(id, Some(token)),
        atMost = timeout
      )
    }

  override def getFutureItems(limit: Int): Seq[SimpleEvent] =
    Await.result(eventSource.getSimpleFutureEventsNotFlaggedByUser(userIdentity.map(_.id), limit), timeout)

  override def getEventsForRange(from: Long, to: Long): Seq[Event] =
    Await.result(eventSource.getEventsBetweenDatesNotFlaggedBy(from, to, userIdentity.map(_.id)), timeout)

  override def joinEventAndGetJoins(eventId: EventId): EventJoinInfo = {
    val messageFuture = eventSource.eventById(eventId).flatMap { event =>
      userIdentity match {
        case Some(UserIdentity(_, id)) if event.isDefined && event.exists(_ isInTheFuture) =>
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
      (userIdentity match {
        //user is logged in
        case Some(UserIdentity(token, id)) => userCache.getOrFetchUserFriends(token, id).map { friends =>
          val (joinedFriends, otherJoins) = allEventJoins.filterNot(_ == id).partition(friends contains)

          //in case the user has more friends than limit
          val joinedFriendsLimited = joinedFriends take limit

          val othersLimited = otherJoins take (limit - joinedFriendsLimited.size)
          joinedFriendsLimited ++ othersLimited
        }
        // user not logged in
        case _ => Future.successful(allEventJoins take limit)
      }).flatMap { joins =>
        Future.sequence(joins.map(userCache.getOrFetchUserInfo(_, userIdentity.map(_.token))))
          .map(_.flatten)
      }
    }

    Await.result(
      joinsFuture,
      atMost = timeout
    )
  }

  override def addEvent(event: Event): EventAddition = userIdentity.map { case UserIdentity(_, uid) =>
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
      userIdentity.map(_.id).map(eventSource.addFlag(eventId, _)).getOrElse(Future.successful(false)),
      atMost = timeout
    )

  private def getUserReputationFuture(id: UserId): Future[UserReputationInfo] = {
    val isTrustedFuture = userCache.isUserTrusted(id)
    val pastJoinsFuture = eventSource.countPastJoinsBy(id)
    val userFuture = userCache.getUserInfo(id)

    for {
      isTrusted <- isTrustedFuture
      pastJoins <- pastJoinsFuture
      user <- userFuture
    } yield {
      buildReputationResponse(isTrusted, pastJoins, user)
    }

  }

  private def buildReputationResponse(isTrusted: Boolean, pastJoins: Int, user: Option[UserInfo]): UserReputationInfo = {
    val name = user.map(_.login).getOrElse("Event Explorer")
    if (!isTrusted) {
      // normal user
      UserReputationInfo(name, pastJoins.toLong + DefaultReputation, ReputationRequiredToAddEvents)
    } else {
      // trusted user
      UserReputationInfo(name, pastJoins.toLong + ReputationRequiredToAddEvents + DefaultReputation, ReputationRequiredToAddEvents)
    }
  }

  override def getUserReputation(): Option[UserReputationInfo] =
    userIdentity.map(_.id).map(getUserReputationFuture).map { rep =>
      Await.result(rep, timeout)
    }
}