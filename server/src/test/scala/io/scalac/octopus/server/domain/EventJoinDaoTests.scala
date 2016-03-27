package io.scalac.octopus.server.domain

import scala.concurrent.ExecutionContext.Implicits.global
import config.DbConfig
import domain.{EventDao, EventFlagDao, EventJoinDao, UserDao}
import io.scalac.octopus.server.{DbSpec, OctoSpec, TestData}
import org.mockito.Mockito._
import slick.driver.PostgresDriver.api._

import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{AlreadyJoined, JoinSuccessful}
import scalac.octopusonwire.shared.domain._

class EventJoinDaoTests extends OctoSpec with DbSpec {
  val dbConfig = mock[DbConfig]

  when(dbConfig.db).thenReturn(db)

  "eventJoinsByUserId" should "return the amount of events the user has joined" in {
    val ejd = new EventJoinDao(dbConfig)
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val sampleUsers = TestData.sampleUsers(2)

    val userJoins = List(1, 2, 3).map(i => EventJoin(EventId(i), UserId(1)))

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= TestData.sampleValidEvents(4),
        userDao.users ++= sampleUsers
      )
    }.flatMap { _ =>
      db.run {
        ejd.allQuery ++= EventJoin(EventId(4), UserId(2)) :: userJoins
      }
    }

    whenReady(fut) { _ =>
      ejd.eventJoinsByUserId(UserId(1)).futureValue should contain theSameElementsAs userJoins
    }
  }

  "getJoiners" should "return all joins for the chosen event" in {
    val ejd = new EventJoinDao(dbConfig)
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= TestData.sampleValidEvents(2),
        userDao.users ++= TestData.sampleUsers(4)
      )
    }.flatMap { _ =>
      db.run {
        ejd.allQuery ++= EventJoin(EventId(2), UserId(4)) :: List(1, 2, 3).map(i => EventJoin(EventId(1), UserId(i)))
      }
    }

    whenReady(fut) { _ =>
      ejd.getJoiners(EventId(1)).futureValue should contain theSameElementsAs Set(1, 2, 3).map(UserId(_))
    }
  }

  "countJoins" should "return the amount of joins for the chosen event" in {
    val ejd = new EventJoinDao(dbConfig)
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= TestData.sampleValidEvents(2),
        userDao.users ++= TestData.sampleUsers(4)
      )
    }.flatMap { _ =>
      db.run {
        ejd.allQuery ++= EventJoin(EventId(2), UserId(4)) :: List(1, 2, 3).map(i => EventJoin(EventId(1), UserId(i)))
      }
    }

    whenReady(fut) { _ =>
      ejd.countJoins(EventId(1)).futureValue shouldBe 3
    }
  }

  "userHasJoinedEvent" should "return true if the user has joined the selected event" in {
    val ejd = new EventJoinDao(dbConfig)
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= TestData.sampleValidEvents(2),
        userDao.users ++= TestData.sampleUsers(2)
      )
    }.flatMap { _ =>
      db.run {
        ejd.allQuery += EventJoin(EventId(1), UserId(1))
      }
    }

    whenReady(fut) { _ =>
      ejd.userHasJoinedEvent(EventId(1), UserId(1)).futureValue shouldBe true
    }
  }

  it should "return false if the user hasn't joined the event" in {
    val ejd = new EventJoinDao(dbConfig)
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= TestData.sampleValidEvents(2),
        userDao.users ++= TestData.sampleUsers(2)
      )
    }.flatMap { _ =>
      db.run {
        ejd.allQuery ++= EventJoin(EventId(2), UserId(1)) :: EventJoin(EventId(1), UserId(2)) :: Nil
      }
    }

    whenReady(fut) { _ =>
      ejd.userHasJoinedEvent(EventId(1), UserId(1)).futureValue shouldBe false
    }
  }

  "joinEvent" should "return false if the user has already joined event" in {
    val ejd = new EventJoinDao(dbConfig)
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= TestData.sampleValidEvents(2),
        userDao.users ++= TestData.sampleUsers(1)
      )
    }.flatMap { _ =>
      db.run {
        ejd.allQuery += EventJoin(EventId(1), UserId(1))
      }
    }

    whenReady(fut) { _ =>
      ejd.joinEvent(EventId(1), UserId(1)).futureValue shouldBe AlreadyJoined.apply
    }
  }

  it should "return true if the user hasn't joined the event before" in {
    val ejd = new EventJoinDao(dbConfig)
    val eventDao = new EventDao(dbConfig, mock[EventJoinDao], mock[EventFlagDao])
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        eventDao.eventQuery ++= TestData.sampleValidEvents(2),
        userDao.users ++= TestData.sampleUsers(1)
      )
    }.flatMap { _ =>
      db.run {
        ejd.allQuery += EventJoin(EventId(2), UserId(1))
      }
    }.flatMap(_ =>
      ejd.joinEvent(EventId(1), UserId(1))
    )

    whenReady(fut) { v =>
      v shouldBe JoinSuccessful.apply
      ejd.userHasJoinedEvent(EventId(1), UserId(1)).futureValue shouldBe true
    }
  }
}
