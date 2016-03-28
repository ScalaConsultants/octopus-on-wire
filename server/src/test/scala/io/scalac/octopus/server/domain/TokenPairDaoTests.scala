package io.scalac.octopus.server.domain

import concurrent.duration._
import slick.driver.PostgresDriver.api._
import domain.{TokenPairDao, UserDao}
import io.scalac.octopus.server.{DbSpec, OctoSpec, TestData}
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scalac.octopusonwire.shared.domain.UserId

class TokenPairDaoTests extends OctoSpec with DbSpec {
  "saveUserToken" should "save user token" in {
    val tpd = new TokenPairDao(dbConfig)
    val userDao = new UserDao(dbConfig)

    db.run {
      userDao.users ++= TestData.sampleUsers(1)
    }.flatMap { _ =>
      tpd.saveUserToken("valid-token", UserId(1))
    }.flatMap { _ =>
      tpd.userIdByToken("valid-token")
    }.futureValue(Timeout(5.seconds)) shouldBe UserId(1)
  }

  it should "update user token" in {
    val tpd = new TokenPairDao(dbConfig)
    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      userDao.users ++= TestData.sampleUsers(1)
    }.flatMap { _ =>
      tpd.saveUserToken("old-token", UserId(1))
    }.flatMap { _ =>
      tpd.saveUserToken("new-token", UserId(1))
    }

    whenReady(fut) { _ =>
      tpd.userIdByToken("new-token").futureValue shouldBe UserId(1)
      tpd.userIdByToken("old-token").failed.futureValue shouldBe an[Exception]
    }
  }

  "userIdByToken" should "fail if there is no user for the requested token" in {
    val tpd = new TokenPairDao(dbConfig)
    val userDao = new UserDao(dbConfig)

    db.run{
      userDao.users ++= TestData.sampleUsers(1)
    }.flatMap{_ =>
      tpd.saveUserToken("some-token", UserId(1))
    }.flatMap{_ =>
      tpd.userIdByToken("non-token")
    }.failed.futureValue shouldBe an[Exception]
  }

  it should "succeed if there is a user for the requested token" in {
    val tpd = new TokenPairDao(dbConfig)
    val userDao = new UserDao(dbConfig)

    db.run{
      userDao.users ++= TestData.sampleUsers(1)
    }.flatMap{_ =>
      tpd.saveUserToken("some-token", UserId(1))
    }.flatMap{_ =>
      tpd.userIdByToken("some-token")
    }.futureValue shouldBe UserId(1)
  }
}
