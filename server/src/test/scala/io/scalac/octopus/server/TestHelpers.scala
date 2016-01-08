package io.scalac.octopus.server

import config.ServerConfig
import data.{InMemoryUserCache, InMemoryEventSource}
import org.scalatest._
import org.scalatest.time.{Hours, Span}
import services.ApiService

import scala.concurrent.Future
import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{Event, EventId, UserId}

private[server] object TestHelpers {
  val inMemoryEventSource = new InMemoryEventSource
  val inMemoryUserCache = new InMemoryUserCache

  val authorizedApi = new ApiService(Some("token"), Some(UserId(1)), inMemoryEventSource, inMemoryUserCache)

  val authorizedApiWithJoinedPastEvents = new ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def countPastJoinsBy(id: UserId): Future[Int] = Future.successful(ServerConfig.PastJoinsRequiredToAddEvents)
  }, inMemoryUserCache)

  val authorizedApiWithOldEvent = new ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def getEvents: List[Event] = oldEvent :: Nil
  }, inMemoryUserCache)

  val authorizedApiWithFutureEvent = new ApiService(Some("token"), Some(UserId(1)), new InMemoryEventSource {
    override def getEvents: List[Event] = sampleValidEvent :: Nil
  }, inMemoryUserCache)

  val unauthorizedApi = new ApiService(None, None, inMemoryEventSource, inMemoryUserCache)

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