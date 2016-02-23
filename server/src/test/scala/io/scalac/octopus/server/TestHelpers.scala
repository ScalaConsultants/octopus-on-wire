package io.scalac.octopus.server

import config.ServerConfig
import data.{InMemoryEventSource, InMemoryUserCache}
import domain.UserIdentity
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Hours, Span}
import services.ApiService

import scala.concurrent.Future
import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{UserInfo, Event, EventId, UserId}

private[server] object TestHelpers {
  val inMemoryEventSource = new InMemoryEventSource
  val inMemoryUserCache = new InMemoryUserCache

  val inMemoryUserCacheWithUser = new InMemoryUserCache{
    override def getUserInfo(id: UserId): Future[UserInfo] = id match{
      case uid@UserId(1) => Future.successful(UserInfo(uid, "Test user"))
      case _ => super.getUserInfo(id)
    }
  }

  class AuthorizedApi extends ApiService(Some(UserIdentity("token", UserId(1))), inMemoryEventSource, inMemoryUserCacheWithUser)

  class AuthorizedApiWithJoinedPastEvents extends ApiService(Some(UserIdentity("token", UserId(1))), new InMemoryEventSource {
    override def countPastJoinsBy(id: UserId): Future[Int] = Future.successful(ServerConfig.ReputationRequiredToAddEvents)
  }, inMemoryUserCacheWithUser)

  class AuthorizedApiWithOldEvent extends ApiService(Some(UserIdentity("token", UserId(1))), new InMemoryEventSource {
    override def getEvents: List[Event] = oldEvent :: Nil
  }, inMemoryUserCache)

  class AuthorizedApiWithFutureEvent extends ApiService(Some(UserIdentity("token", UserId(1))), new InMemoryEventSource {
    override def getEvents: List[Event] = sampleValidEvent :: Nil
  }, inMemoryUserCache)

  class UnauthorizedApi extends ApiService(None, inMemoryEventSource, inMemoryUserCache)

  val sampleValidEvent = {
    val start = System.currentTimeMillis() + 10.hours.toMillis
    val end = start + Span(5, Hours).toMillis
    Event(EventId(0), "Some valid event", start, end, 0, "Somewhere", "http://example.com")
  }
  val oldEvent = {
    val start = System.currentTimeMillis() - 10.hours.toMillis
    val end = start + 5.seconds.toMillis
    Event(EventId(0), "Some invalid event", start, end, 0, "Hell", "http://microsoft.com")
  }
}

trait OctoSpec extends FlatSpec with ScalaFutures with ShouldMatchers