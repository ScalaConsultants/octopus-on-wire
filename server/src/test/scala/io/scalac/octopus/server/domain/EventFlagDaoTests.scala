package io.scalac.octopus.server.domain

import config.DbConfig
import domain.{EventDao, EventFlagDao, EventJoinDao, UserDao}
import io.scalac.octopus.server.{DbSpec, OctoSpec, TestData}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import slick.driver.PostgresDriver.api._

import scalac.octopusonwire.shared.domain.{EventFlag, EventId, UserId, UserInfo}

class EventFlagDaoTests extends OctoSpec with BeforeAndAfterEach with DbSpec {
  val dbConfig = mock[DbConfig]

  when(dbConfig.db).thenReturn(db)

  "getFlaggers" should "return some users" in {
    val efd = new EventFlagDao(dbConfig)

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val sampleUsers = List(1, 2, 3).map(i => UserInfo(UserId(i), s"test$i"))

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery += TestData.getSampleValidEvent,
        userDao.users ++= sampleUsers,
        efd.allQuery ++= sampleUsers.map(_.userId).map(id => EventFlag(EventId(1), id))
      )
    }

    whenReady(fut) { _ =>
      efd.getFlaggers(EventId(1)).futureValue should contain theSameElementsAs Set(1, 2, 3).map(UserId(_))
    }
  }

  "userHasFlaggedEvent" should "return true if the user has flagged the event" in {
    val efd = new EventFlagDao(dbConfig)

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery += TestData.getSampleValidEvent,
        userDao.users += UserInfo(UserId(1), "test"),
        efd.allQuery += EventFlag(EventId(1), UserId(1))
      )
    }

    whenReady(fut) { _ =>
      efd.userHasFlaggedEvent(EventId(1), UserId(1)).futureValue shouldBe true
    }
  }

  it should "return false if the user hasn't flagged the event" in {
    val efd = new EventFlagDao(dbConfig)

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery += TestData.getSampleValidEvent,
        userDao.users ++= List(1, 2).map(id => UserInfo(UserId(id), s"test$id")),
        efd.allQuery += EventFlag(EventId(1), UserId(2))
      )
    }

    whenReady(fut) { _ =>
      efd.userHasFlaggedEvent(EventId(1), UserId(1)).futureValue shouldBe false
    }
  }

  "eventFlagsByUserId" should "return some flags when appropriate" in {
    val efd = new EventFlagDao(dbConfig)

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val sampleEvents = List(1, 2, 3, 4).map(id => TestData.getSampleValidEvent.copy(id = EventId(id)))

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= sampleEvents,
        userDao.users ++= List(1, 2).map(id => UserInfo(UserId(id), s"test$id")),
        efd.allQuery ++=
          EventFlag(EventId(4), UserId(2)) :: List(1, 2, 3).map(eid => EventFlag(EventId(eid), UserId(1)))
      )
    }

    val expectedFlags = List(1, 2, 3).map(eid => EventFlag(EventId(eid), UserId(1)))

    whenReady(fut) { _ =>
      efd.eventFlagsByUserId(UserId(1)).futureValue should contain theSameElementsAs expectedFlags
    }
  }

  "flagEvent" should "return false if the event is already flagged" in {
    val efd = new EventFlagDao(dbConfig)

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery += TestData.getSampleValidEvent,
        userDao.users += UserInfo(UserId(1), "test"),
        efd.allQuery += EventFlag(EventId(1), UserId(1))
      )
    }

    whenReady(fut) { _ =>
      efd.flagEvent(EventId(1), UserId(1)).futureValue shouldBe false
    }
  }

  it should "return true if the event wasn't flagged by the user before" in {
    val efd = new EventFlagDao(dbConfig)

    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val sampleEvents = List(1, 2).map(id => TestData.getSampleValidEvent.copy(id = EventId(id)))
    val sampleUsers = List(1, 2).map(id => UserInfo(UserId(id), s"test$id"))

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= sampleEvents,
        userDao.users ++= sampleUsers,
        efd.allQuery ++= EventFlag(EventId(1), UserId(2)) :: EventFlag(EventId(2), UserId(1)) :: Nil
      )
    }

    whenReady(fut) { _ =>
      efd.flagEvent(EventId(1), UserId(1)).futureValue shouldBe true
    }
  }

}
