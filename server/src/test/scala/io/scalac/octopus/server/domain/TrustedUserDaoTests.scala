package io.scalac.octopus.server.domain

import domain.{TrustedUser, TrustedUserDao, UserDao}
import io.scalac.octopus.server.{DbSpec, OctoSpec, TestData}
import slick.driver.PostgresDriver.api._

import scalac.octopusonwire.shared.domain.UserId

class TrustedUserDaoTests extends OctoSpec with DbSpec {
  "isUserTrusted" should "return false if the user is not trusted" in {
    val tud = new TrustedUserDao(dbConfig)

    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        userDao.users ++= TestData.sampleUsers(1),
        tud.trustedUsers += TrustedUser(UserId(1))
      )
    }

    whenReady(fut){_ =>
      tud.isUserTrusted(UserId(2)).futureValue shouldBe false
    }
  }

  it should "return true if the user is trusted" in {
    val tud = new TrustedUserDao(dbConfig)

    val userDao = new UserDao(dbConfig)

    val fut = db.run {
      DBIO.seq(
        userDao.users ++= TestData.sampleUsers(1),
        tud.trustedUsers += TrustedUser(UserId(1))
      )
    }

    whenReady(fut){_ =>
      tud.isUserTrusted(UserId(1)).futureValue shouldBe true
    }
  }
}
