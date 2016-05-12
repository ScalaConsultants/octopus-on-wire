package io.scalac.octopus.server.data

import scala.concurrent.ExecutionContext.Implicits.global
import data.PersistentUserCache
import domain.{TokenPairDao, TrustedUserDao, UserDao, UserFriendPairDao}
import io.scalac.octopus.server.OctoSpec
import org.mockito.Mockito._
import services.GithubApi

import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class PersistentUserCacheTests extends OctoSpec {
  def boilerplate = (mock[TokenPairDao], mock[TrustedUserDao], mock[UserFriendPairDao], mock[UserDao], mock[GithubApi])

  //this one is for the 100% coverage ;)
  "githubApi" should "exist" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    uc.githubApi shouldBe a[GithubApi]
  }

  "isUserTrusted" should "forward result from trusted" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    when(trusted.isUserTrusted(UserId(1))).thenReturnFuture(true)
    when(trusted.isUserTrusted(UserId(2))).thenReturnFuture(false)

    uc.isUserTrusted(UserId(1)).futureValue shouldBe true
    uc.isUserTrusted(UserId(2)).futureValue shouldBe false
  }

  "getUserInfo" should "return user info if user exists" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    val uinfo = UserInfo(UserId(1), "username")

    when(users.userById(UserId(1))).thenReturnFuture(Some(uinfo))

    uc.getUserInfo(UserId(1)).futureValue shouldBe Some(uinfo)
  }

  it should "fail if the user doesn't exist" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    when(users.userById(UserId(2))).thenReturnFuture(None)

    uc.getUserInfo(UserId(2)).futureValue shouldBe None
  }

  "saveUserInfo" should "save user info" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    val uinfo = UserInfo(UserId(1), "username")

    when(users.saveUserInfo(uinfo)).thenReturnFuture(1)
    whenReady(uc.saveUserInfo(uinfo)) { joined =>
      joined shouldBe 1
      verify(users).saveUserInfo(uinfo)
    }
  }

  "saveUserToken" should "save user token" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    when(tokens.saveUserToken("some-token", UserId(1))).thenReturnFuture(1)

    whenReady(uc.saveUserToken("some-token", UserId(1))) { _ =>
      verify(tokens).saveUserToken("some-token", UserId(1))
    }
  }

  "getUserIdByToken" should "return a user id if given valid token" in {
    val (tokens, trusted, friends, users, gh) = boilerplate
    val token = "some-token"

    when(tokens.userIdByToken(token)).thenReturnFuture(Some(UserId(1)))

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    uc.getUserIdByToken(token).futureValue shouldBe Some(UserId(1))
  }

  it should "fail if given invalid token" in {
    val (tokens, trusted, friends, users, gh) = boilerplate
    val token = "bad-token"

    when(tokens.userIdByToken(token)).thenReturnFuture(None)

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    uc.getUserIdByToken(token).futureValue shouldBe None
  }

  "getUserFriends" should "return some friends for existing users" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    when(friends.getUserFriends(UserId(1))).thenReturnFuture(Set(2, 3, 4).map(UserId(_)))

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    uc.getUserFriends(UserId(1)).futureValue shouldBe Some(Set(2, 3, 4).map(UserId(_)))
  }

  it should "fail if the list is empty" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    when(friends.getUserFriends(UserId(1))).thenReturnFuture(Set.empty[UserId])

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)

    uc.getUserFriends(UserId(1)).futureValue shouldBe None
  }

  "saveUserFriends" should "save friends" in {
    val (tokens, trusted, friends, users, gh) = boilerplate

    val ids = Set(2, 3, 4).map(UserId(_))

    ids.foreach { id =>
      when(users.userById(id)).thenReturnFuture(Some(UserInfo(id, "username")))
    }

    val uc = new PersistentUserCache(tokens, trusted, friends, users, gh)
    whenReady(uc.saveUserFriends(UserId(1), ids, "some-token")) { _ =>
      verify(friends).saveUserFriends(UserId(1), ids)
    }
  }

}
