package io.scalac.octopus.server.domain

import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import domain.UserDao
import io.scalac.octopus.server.{DbSpec, OctoSpec}

import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class UserDaoTests extends OctoSpec with DbSpec {
  "saveUserInfo" should "save user info" in {
    val user = UserInfo(UserId(1), "some-user")
    val userDao = new UserDao(dbConfig)

    userDao.saveUserInfo(user).flatMap { _ =>
      db.run {
        userDao.users.result
      }
    }.futureValue should contain theSameElementsAs Set(user)
  }

  it should "override previously saved info" in {
    val oldUser = UserInfo(UserId(1), "some-user")
    val userDao = new UserDao(dbConfig)
    val newUser = oldUser.copy(login = "new-user-name")

    val result = for {
      _ <- userDao.saveUserInfo(oldUser)
      _ <- userDao.saveUserInfo(newUser)
      users <- db.run {
        userDao.users.result
      }
    } yield users

    result.futureValue should contain theSameElementsAs Set(newUser)
  }

  "userById" should "succeed when appropriate" in {
    val user = UserInfo(UserId(1), "some-uer")
    val userDao = new UserDao(dbConfig)

    val result = db.run {
      userDao.users += user
    }.flatMap { _ =>
      userDao.userById(UserId(1))
    }

    result.futureValue shouldBe user
  }

  it should "fail when there is no user for the id requested" in {
    val user = UserInfo(UserId(1), "some-uer")
    val userDao = new UserDao(dbConfig)

    val result = db.run {
      userDao.users += user
    }.flatMap { _ =>
      userDao.userById(UserId(2))
    }

    result.failed.futureValue shouldBe an[Exception]
  }
}
