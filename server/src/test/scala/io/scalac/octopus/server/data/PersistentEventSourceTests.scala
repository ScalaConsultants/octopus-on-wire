package io.scalac.octopus.server.data

import concurrent.duration._
import data.PersistentEventSource
import domain.{EventDao, EventFlagDao, EventJoinDao}
import io.scalac.octopus.server.{OctoSpec, TestData}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import tools.OffsetTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.JoinSuccessful
import scalac.octopusonwire.shared.domain._

class PersistentEventSourceTests extends OctoSpec {
  private def boilerplate = (mock[EventDao], mock[EventJoinDao], mock[EventFlagDao])

  "countPastJoinsBy" should "return the underlying value" in {
    val (events, joins, flags) = boilerplate

    when(events.countPastJoinsBy(UserId(1), OffsetTime(any[Long], any[Long]))) thenReturnFuture 3

    val es = new PersistentEventSource(events, joins, flags)

    es.countPastJoinsBy(UserId(1)).futureValue shouldBe 3
  }

  "countJoins" should "return the underlying value" in {
    val (events, joins, flags) = (mock[EventDao], mockDeep[EventJoinDao], mock[EventFlagDao])

    when(joins.countJoins.apply(EventId(1))) thenReturnFuture 10

    val es = new PersistentEventSource(events, joins, flags)
    es.countJoins(EventId(1)).futureValue shouldBe 10
  }

  it should "fail when given invalid id" in {
    val (events, joins, flags) = (mock[EventDao], mockDeep[EventJoinDao], mock[EventFlagDao])

    when(joins.countJoins.apply(EventId(10000))) thenReturn Future.failed(new Exception("Event doesn't exist"))

    val es = new PersistentEventSource(events, joins, flags)
    es.countJoins(EventId(10000)).failed.futureValue shouldBe an[Exception]
  }

  "hasUserJoinedEvent" should "return the underlying value" in {
    val (events, joins, flags) = (mock[EventDao], mockDeep[EventJoinDao], mock[EventFlagDao])

    when(joins.userHasJoinedEvent.apply(EventId(1), UserId(1))) thenReturnFuture true

    val es = new PersistentEventSource(events, joins, flags)
    es.hasUserJoinedEvent(EventId(1), UserId(1)).futureValue shouldBe true
  }

  it should "return false if there are no joins by the user for the event" in {
    val (events, joins, flags) = (mock[EventDao], mockDeep[EventJoinDao], mock[EventFlagDao])

    when(joins.userHasJoinedEvent.apply(EventId(1), UserId(1))) thenReturnFuture false

    val es = new PersistentEventSource(events, joins, flags)
    es.hasUserJoinedEvent(EventId(1), UserId(1)).futureValue shouldBe false
  }

  "getEventsBetweenDatesNotFlaggedBy" should "return some events when there is a user with flags" in {
    val (events, joins, flags) = boilerplate

    val now = OffsetTime(System.currentTimeMillis(), 0).value
    val start = now + 1.hour.toMillis
    val end = now + 10.hours.toMillis
    val uid = Some(UserId(1))
    val event = TestData.getSampleValidEvent.copy(id = EventId(1))

    when(events.getEventsBetweenDatesNotFlaggedBy(start, end, uid)).thenReturnFuture(List(event))

    val es = new PersistentEventSource(events, joins, flags)
    es.getEventsBetweenDatesNotFlaggedBy(start, end, uid).futureValue should contain theSameElementsAs List(event)
  }

  it should "return all events between dates when there is no user" in {
    val (events, joins, flags) = boilerplate

    val now = OffsetTime(System.currentTimeMillis(), 0).value
    val start = now + 1.hour.toMillis
    val end = now + 10.hours.toMillis
    val uid = None

    val event = TestData.getSampleValidEvent.copy(id = EventId(1), startDate = start, endDate = end)
    val event2 = event.copy(id = EventId(2))

    when(events.getEventsBetweenDatesNotFlaggedBy(start, end, uid)).thenReturnFuture(List(event, event2))

    val es = new PersistentEventSource(events, joins, flags)
    es.getEventsBetweenDatesNotFlaggedBy(start, end, uid).futureValue should contain theSameElementsAs List(event, event2)
  }

  "getSimpleFutureEventsNotFlaggedByUser" should "return some future events when there is a user with flags" in {
    val (events, joins, flags) = boilerplate

    val now = OffsetTime(System.currentTimeMillis(), 0).value
    val start = now + 1.hour.toMillis
    val end = now + 10.hours.toMillis
    val uid = Some(UserId(1))
    val event = TestData.getSampleValidEvent.copy(id = EventId(1), startDate = start, endDate = end)

    when(events.getFutureUnflaggedEvents(Matchers.eq(uid), 3, OffsetTime(any[Long], any[Long]))).thenReturnFuture(List(event.toSimple))

    val es = new PersistentEventSource(events, joins, flags)
    es.getSimpleFutureEventsNotFlaggedByUser(uid, 3).futureValue should contain theSameElementsAs List(event.toSimple)
  }

  it should "return up to {limit} future events when there is no user" in {
    val (events, joins, flags) = boilerplate

    val now = OffsetTime(System.currentTimeMillis(), 0).value
    val start = now + 1.hour.toMillis
    val end = now + 10.hours.toMillis
    val uid = None
    val event = TestData.getSampleValidEvent.copy(id = EventId(1), startDate = start, endDate = end)
    val event2 = event.copy(id = EventId(2))

    when(events.getFutureUnflaggedEvents(Matchers.eq(uid), 3, OffsetTime(any[Long], any[Long])))
      .thenReturnFuture(List(event, event2).map(_.toSimple))

    val es = new PersistentEventSource(events, joins, flags)
    es.getSimpleFutureEventsNotFlaggedByUser(uid, 3)
      .futureValue should contain theSameElementsAs List(event, event2).map(_.toSimple)
  }

  "joinEvent" should "forward the message from EventJoinDao" in {
    val (events, joins, flags) = boilerplate

    val es = new PersistentEventSource(events, joins, flags)

    EventJoinMessageBuilder.allStates foreach { state =>
      when(joins.joinEvent(EventId(1), UserId(1))).thenReturnFuture(state.apply)

      es.joinEvent(UserId(1), EventId(1)).futureValue should be(state.apply)
    }
  }

  "getJoins" should "return some joins when called" in {
    val (events, joins, flags) = boilerplate

    val allIds = Set(1, 2, 3, 4, 5).map(UserId(_))

    val es = new PersistentEventSource(events, joins, flags)

    when(joins.getJoiners(EventId(1))).thenReturnFuture(allIds)

    es.getJoins(EventId(1)).futureValue should be(allIds)
  }

  "eventById" should "return some event when given existing id" in {
    val (events, joins, flags) = boilerplate

    val event = TestData.getSampleValidEvent.copy(id = EventId(1))

    when(events.findEventById(EventId(1))).thenReturnFuture(event)
    val es = new PersistentEventSource(events, joins, flags)

    es.eventById(EventId(1)).futureValue shouldBe event
  }

  it should "fail when given nonexistent id" in {
    val (events, joins, flags) = boilerplate

    when(events.findEventById(EventId(999))).thenReturn(Future.failed(new Exception("Event not found")))
    val es = new PersistentEventSource(events, joins, flags)

    es.eventById(EventId(999)).failed.futureValue shouldBe an[Exception]
  }

  "addEvent" should "add event when given new event id" in {
    val (events, joins, flags) = boilerplate

    val event = TestData.getSampleValidEvent

    when(events.addEventAndGetId(event)).thenReturnFuture(EventId(2))
    val es = new PersistentEventSource(events, joins, flags)

    es.addEvent(event).futureValue shouldBe Added()
  }

  it should "return a FailedToAdd message when an error occurs" in {
    val (events, joins, flags) = boilerplate

    val event = TestData.getSampleValidEvent.copy(id = EventId(1))

    when(events.addEventAndGetId(event)).thenReturnFuture(NoId)
    val es = new PersistentEventSource(events, joins, flags)

    es.addEvent(event).futureValue shouldBe FailedToAdd("Unknown error")
  }

  "addFlag" should "add flags to an existing event when it's not flagged by user" in {
    val (events, joins, flags) = boilerplate

    val es = new PersistentEventSource(events, joins, flags)

    when(events.eventExists(EventId(1))).thenReturnFuture(true)
    when(flags.flagEvent(EventId(1), UserId(1))).thenReturnFuture(true)

    es.addFlag(EventId(1), UserId(1)).futureValue shouldBe true
  }

  it should "not add flags to an existing event when it's flagged by user" in {
    val (events, joins, flags) = boilerplate

    val es = new PersistentEventSource(events, joins, flags)

    when(events.eventExists(EventId(1))).thenReturnFuture(true)
    when(flags.flagEvent(EventId(1), UserId(1))).thenReturnFuture(false)

    es.addFlag(EventId(1), UserId(1)).futureValue shouldBe false
  }

  it should "not add flags to a nonexistent event" in {
    val (events, joins, flags) = boilerplate

    val es = new PersistentEventSource(events, joins, flags)

    when(events.eventExists(EventId(1))).thenReturnFuture(false)

    es.addFlag(EventId(1), UserId(1)).futureValue shouldBe false
  }
}
