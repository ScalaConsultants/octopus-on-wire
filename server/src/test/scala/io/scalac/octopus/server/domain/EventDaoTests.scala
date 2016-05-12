package io.scalac.octopus.server.domain

import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.PostgresDriver.api._
import org.mockito.Mockito._
import config.DbConfig
import domain.{EventDao, EventFlagDao, EventJoinDao}
import io.scalac.octopus.server.{DbSpec, OctoSpec, TestData}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import tools.{OffsetTime, TimeHelpers}

import concurrent.duration._
import scalac.octopusonwire.shared.domain.{EventFlag, EventId, EventJoin, UserId}

class EventDaoTests extends OctoSpec with DbSpec {
  "findEventById" should "work if there is an event for given id" in {
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])

    val newEvent = TestData.getSampleValidEvent.copy(id = EventId(1))

    db.run {
      eventDao.eventQuery += newEvent
    }.flatMap { _ =>
      eventDao.findEventById(EventId(1))
    }.futureValue(Timeout(5.seconds)) shouldBe Some(newEvent)
  }

  it should "fail if there is no event for given id" in {
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])

    db.run {
      eventDao.eventQuery += TestData.getSampleValidEvent
    }.flatMap { _ =>
      eventDao.findEventById(EventId(2))
    }.futureValue shouldBe None

  }

  "eventExists" should "return true if there is an event for the given id" in {
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])

    db.run {
      eventDao.eventQuery += TestData.getSampleValidEvent
    }.flatMap { _ =>
      eventDao.eventExists(EventId(1))
    }.futureValue shouldBe true
  }

  it should "return false if there is no event for the given id" in {
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])

    db.run {
      eventDao.eventQuery += TestData.getSampleValidEvent
    }.flatMap { _ =>
      eventDao.eventExists(EventId(2))
    }.futureValue shouldBe false
  }

  "countPastJoinsBy" should "return some number when there are joins" in {
    val joinDao = mock[EventJoinDao]
    val eventDao = new EventDao(dbConfig, joinDao, mock[EventFlagDao])

    val sampleEvents = TestData.sampleValidEvents(4).zipWithIndex.map { case (ev, i) => ev.copy(id = EventId(i)) }

    val now = sampleEvents.head.endDate + 10.hours.toMillis

    val futureEvent = sampleEvents.head.copy(endDate = now + 1.hours.toMillis)

    when(joinDao.eventJoinsByUserId(UserId(1)))
      .thenReturnFuture(List(1, 2, 3, 4).map(i => EventJoin(EventId(i), UserId(1))))

    db.run {
      eventDao.eventQuery ++= futureEvent :: sampleEvents
    }.flatMap { _ =>
      eventDao.countPastJoinsBy(UserId(1), OffsetTime(now, sampleEvents.head.offset))
    }.futureValue(Timeout(5.seconds)) shouldBe 3
  }

  "getEventsBetweenDatesNotFlaggedBy" should "return all events between dates when there is no user" in {
    val efd = mock[EventFlagDao]

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], efd)

    val sampleEvents = TestData.sampleValidEvents(4).zipWithIndex.map { case (ev, i) => ev.copy(id = EventId(i + 2)) }

    val now = sampleEvents.head.endDate - 2.hours.toMillis

    val from = now - 2.hours.toMillis
    val to = now + 5.days.toMillis

    val futureEvent = sampleEvents.head.copy(startDate = now + 9.days.toMillis, endDate = now + 10.days.toMillis)

    db.run {
      eventDao.eventQuery ++= futureEvent :: sampleEvents
    }.flatMap { _ =>
      eventDao.getEventsBetweenDatesNotFlaggedBy(from, to, None)
    }.futureValue should contain theSameElementsAs sampleEvents
  }

  it should "return some events if there is a user" in {
    val efd = mockDeep[EventFlagDao]

    when(efd.eventFlagsByUserId.apply(UserId(1))).thenReturnFuture(List(1, 2).map(i => EventFlag(EventId(i), UserId(1))))

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], efd)

    val sampleEvents = TestData.sampleValidEvents(4).zipWithIndex.map { case (ev, i) => ev.copy(id = EventId(i + 2)) }

    val now = sampleEvents.head.endDate - 2.hours.toMillis

    val from = now - 2.hours.toMillis
    val to = now + 5.days.toMillis

    val futureEvent = sampleEvents.head.copy(startDate = now + 9.days.toMillis, endDate = now + 10.days.toMillis)

    db.run {
      eventDao.eventQuery ++= futureEvent :: sampleEvents
    }.flatMap { _ =>
      eventDao.getEventsBetweenDatesNotFlaggedBy(from, to, Some(UserId(1)))
    }.futureValue should contain theSameElementsAs sampleEvents.tail
  }

  "getFutureUnflaggedEvents" should "return all future events when there is no user" in {
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])

    val sampleEvents = TestData.sampleValidEvents(4).zipWithIndex.map { case (ev, i) => ev.copy(id = EventId(i + 2)) }

    val now = OffsetTime.apply(sampleEvents.head.endDate - 2.hours.toMillis, TimeHelpers.readServerOffset())

    db.run {
      eventDao.eventQuery ++= TestData.oldEvent :: sampleEvents
    }.flatMap { _ =>
      eventDao.getFutureUnflaggedEvents(None, 3, now)
    }.futureValue should contain theSameElementsAs sampleEvents.map(_.toSimple).take(3)
  }

  it should "return some events if there is a user" in {
    val efd = mockDeep[EventFlagDao]

    when(efd.eventFlagsByUserId.apply(UserId(1))).thenReturnFuture(EventFlag(EventId(2), UserId(1)) :: Nil)

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], efd)

    val sampleEvents = TestData.sampleValidEvents(4).zipWithIndex.map { case (ev, i) => ev.copy(id = EventId(i + 2)) }

    val now = OffsetTime.apply(sampleEvents.head.endDate - 2.hours.toMillis, TimeHelpers.readServerOffset())

    db.run {
      eventDao.eventQuery ++= TestData.oldEvent :: sampleEvents
    }.flatMap { _ =>
      eventDao.getFutureUnflaggedEvents(Some(UserId(1)), 3, now)
    }.futureValue should contain theSameElementsAs sampleEvents.map(_.toSimple).slice(1, 4)
  }

  "addEventAndGetId" should "add event and get its id" in {
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])

    val event = TestData.getSampleValidEvent

    eventDao.addEventAndGetId(event).futureValue shouldBe EventId(1)
  }

  it should "add event and get its id even if there are events already" in {
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])

    val sampleEvent = TestData.getSampleValidEvent

    db.run{
      eventDao.eventQuery += sampleEvent
    }.flatMap{_ =>
      eventDao.addEventAndGetId(sampleEvent)
    }.futureValue shouldBe EventId(2)
  }
}
