package io.scalac.octopus.server

import java.io.IOException

import config.ServerConfig
import data.{InMemoryEventSource, InMemoryUserCache}
import org.scalatest._
import org.scalatest.time.{Hours, Span}
import play.api.libs.ws.{WSClient, WSRequest}
import services.{ApiService, GithubApi}

import scala.concurrent.Future
import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{Event, EventId, UserId}

private[server] object TestHelpers {
  val inMemoryEventSource = new InMemoryEventSource

  val mockWSClient: WSClient = new WSClient {
    override def underlying[T]: T = ???

    override def url(url: String): WSRequest = ???

    @throws[IOException]
    override def close(): Unit = ???
  }

  val inMemoryUserCache = new InMemoryUserCache(new GithubApi(mockWSClient))

  class AuthorizedApi extends ApiService(Some("token"), Some(UserId(1)), inMemoryEventSource, inMemoryUserCache)

  class AuthorizedApiWithJoinedPastEvents extends ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def countPastJoinsBy(id: UserId): Future[Int] = Future.successful(ServerConfig.ReputationRequiredToAddEvents)
  }, inMemoryUserCache)

  class AuthorizedApiWithOldEvent extends ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def getEvents: List[Event] = oldEvent :: Nil
  }, inMemoryUserCache)

  class AuthorizedApiWithFutureEvent extends ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def getEvents: List[Event] = sampleValidEvent :: Nil
  }, inMemoryUserCache)

  class UnauthorizedApi extends ApiService(None, None, inMemoryEventSource, inMemoryUserCache)

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

trait OctoSpec extends FlatSpec with ShouldMatchers