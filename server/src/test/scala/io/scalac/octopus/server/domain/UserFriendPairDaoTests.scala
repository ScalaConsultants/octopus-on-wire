package io.scalac.octopus.server.domain

import domain.{UserDao, UserFriendPairDao}
import io.scalac.octopus.server.{DbSpec, OctoSpec, TestData}
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scalac.octopusonwire.shared.domain.{UserFriendPair, UserId}

class UserFriendPairDaoTests extends OctoSpec with DbSpec {
  "getUserFriends" should "return some friends" in {
    val ufp = new UserFriendPairDao(dbConfig)
    val userDao = new UserDao(dbConfig)

    val sampleUsers = TestData.sampleUsers(4)

    val friendPairs = List(2, 3, 4).map(i => UserFriendPair(UserId(1), UserId(i)))

    db.run {
      DBIO.seq(
        userDao.users ++= sampleUsers,
        ufp.userFriendPairs ++= friendPairs
      )
    }

    ufp.getUserFriends(UserId(1)).futureValue should contain theSameElementsAs Set(2, 3, 4).map(UserId(_))
  }

  "saveUserFriends" should "add friends" in {
    val ufp = new UserFriendPairDao(dbConfig)
    val userDao = new UserDao(dbConfig)

    val sampleUsers = TestData.sampleUsers(5)

    val userFriends = sampleUsers.tail.map(_.userId).toSet

    def addUsers = db.run {
      userDao.users ++= sampleUsers
    }

    def saveFriends = ufp.saveUserFriends(UserId(1), userFriends)

    def getFriends = db.run {
      ufp.userFriendPairs.result
    }


    val friendPairs = userFriends.map(friend => UserFriendPair(UserId(1), friend))

    val result = for {
      _ <- addUsers
      _ <- saveFriends
      savedFriends <- getFriends
    } yield savedFriends

    result.futureValue should contain theSameElementsAs friendPairs

  }
}
