package io.scalac.octopus.server.data

import data.UserCache
import domain.UserIdentity
import io.scalac.octopus.server.OctoSpec
import org.mockito.Matchers._
import org.mockito.Mockito._
import services.GithubApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class UserCacheTests extends OctoSpec {

  abstract class AbstractUserCache extends UserCache

  "getOrFetchUserInfo" should "not fetch if info is in the db" in {
    val userCache = mock[AbstractUserCache]
    val info = UserInfo(UserId(1), "username")

    when(userCache.getOrFetchUserInfo(UserId(1), None)).thenCallRealMethod()
    when(userCache.getUserInfo(UserId(1))).thenReturnFuture(Some(info))

    whenReady(userCache.getOrFetchUserInfo(UserId(1), None)) { v =>
      v shouldBe info

      verify(userCache).getOrFetchUserInfo(UserId(1), None)
      verify(userCache, times(0)).fetchUserInfo(UserId(1), None)
      verify(userCache, times(0)).saveUserInfo(any[UserInfo])
    }
  }

  it should "fetch when needed and fail if the user doesn't exist" in {

    val userCache = mock[AbstractUserCache]

    when(userCache.getOrFetchUserInfo(UserId(999), None)).thenCallRealMethod()
    when(userCache.getUserInfo(UserId(999))).thenReturnFuture(None)
    when(userCache.fetchUserInfo(UserId(999), None)).thenReturn(Future.failed(new Exception("User not found")))

    whenReady(userCache.getOrFetchUserInfo(UserId(999), None).failed) { v =>
      v shouldBe an[Exception]

      verify(userCache).getOrFetchUserInfo(UserId(999), None)
      verify(userCache).fetchUserInfo(UserId(999), None)
      verify(userCache, times(0)).saveUserInfo(any[UserInfo])
    }
  }

  it should "fetch when needed and save user info if the user exists (on GH)" in {

    val info = UserInfo(UserId(1), "username")

    val userCache = mock[AbstractUserCache]

    when(userCache.getOrFetchUserInfo(UserId(999), None)).thenCallRealMethod()
    when(userCache.getUserInfo(UserId(999))).thenReturnFuture(None)
    when(userCache.fetchUserInfo(UserId(999), None)).thenReturnFuture(info)

    whenReady(userCache.getOrFetchUserInfo(UserId(999), None)) { v =>
      v shouldBe info

      verify(userCache).getOrFetchUserInfo(UserId(999), None)
      verify(userCache).fetchUserInfo(UserId(999), None)
      verify(userCache).saveUserInfo(info)
    }
  }

  "getOrFetchUserId" should "not fetch if id is in the db" in {
    val userCache = mock[AbstractUserCache]
    val token = "some-token"

    when(userCache.getOrFetchUserId(token)).thenCallRealMethod()
    when(userCache.getUserIdByToken(token)).thenReturnFuture(Some(UserId(1)))

    whenReady(userCache.getOrFetchUserId(token)) { v =>
      v shouldBe UserId(1)
      verify(userCache).getUserIdByToken(token)
      verify(userCache, times(0)).fetchCurrentUserInfo(token)
      verify(userCache, times(0)).saveUserInfo(any[UserInfo])
      verify(userCache, times(0)).saveUserToken(token, UserId(1))
    }
  }

  it should "fetch when needed and fail if the token is invalid" in {
    val userCache = mock[AbstractUserCache]
    val token = "some-token"

    when(userCache.getOrFetchUserId(token)).thenCallRealMethod()
    when(userCache.getUserIdByToken(token)).thenReturnFuture(None)
    when(userCache.fetchCurrentUserInfo(token)).thenReturn(Future.failed(new Exception("Invalid token")))

    whenReady(userCache.getOrFetchUserId(token).failed) { v =>
      v shouldBe an[Exception]

      verify(userCache).fetchCurrentUserInfo(token)
      verify(userCache, times(0)).saveUserInfo(any[UserInfo])
      verify(userCache, times(0)).saveUserToken(token, UserId(1))
    }
  }

  it should "fetch when needed and save info if the token is valid" in {
    val userCache = mock[AbstractUserCache]
    val token = "some-token"

    val info = UserInfo(UserId(1), "username")

    when(userCache.getOrFetchUserId(token)).thenCallRealMethod()
    when(userCache.getUserIdByToken(token)).thenReturnFuture(None)
    when(userCache.fetchCurrentUserInfo(token)).thenReturnFuture(info)

    whenReady(userCache.getOrFetchUserId(token)) { v =>
      v shouldBe UserId(1)
      verify(userCache).getUserIdByToken(token)
      verify(userCache).fetchCurrentUserInfo(token)
      verify(userCache).saveUserInfo(info)
      verify(userCache).saveUserToken(token, UserId(1))
    }
  }

  "getOrFetchUserFriends" should "not fetch if there are friends in the db" in {
    val userCache = mock[AbstractUserCache]
    val ident = UserIdentity("some-token", UserId(1))
    val ids = Set(2, 3, 4).map(UserId(_))

    when(userCache.getOrFetchUserFriends(ident)).thenCallRealMethod()
    when(userCache.getUserFriends(UserId(1))).thenReturnFuture(Some(ids))

    whenReady(userCache.getOrFetchUserFriends(ident)) { v =>
      v should contain theSameElementsAs ids
      verify(userCache).getUserFriends(UserId(1))
      verify(userCache, times(0)).fetchUserFriends("some-token")
      verify(userCache, times(0)).saveUserFriends(UserId(1), ids, "some-token")
    }
  }

  it should "fetch when needed and fail if the token is invalid" in {
    val userCache = mock[AbstractUserCache]
    val ident = UserIdentity("some-token", UserId(1))

    when(userCache.getOrFetchUserFriends(ident)).thenCallRealMethod()
    when(userCache.getUserFriends(UserId(1))).thenReturnFuture(None)
    when(userCache.fetchUserFriends("some-token")).thenReturn(Future.failed(new Exception("Invalid token")))

    whenReady(userCache.getOrFetchUserFriends(ident).failed) { v =>
      v shouldBe an[Exception]
      verify(userCache).getUserFriends(UserId(1))
      verify(userCache).fetchUserFriends("some-token")
      verify(userCache, times(0)).saveUserFriends(any[UserId], any[Set[UserId]], any[String])
    }
  }

  it should "fetch ween needed and save friends if the token is valid" in {
    val userCache = mock[AbstractUserCache]
    val ident = UserIdentity("some-token", UserId(1))
    val ids = Set(2, 3, 4).map(UserId(_))

    when(userCache.getOrFetchUserFriends(ident)).thenCallRealMethod()
    when(userCache.getUserFriends(UserId(1))).thenReturnFuture(None)
    when(userCache.fetchUserFriends("some-token")).thenReturnFuture(ids)

    whenReady(userCache.getOrFetchUserFriends(ident)) { v =>
      v should contain theSameElementsAs ids
      verify(userCache).getUserFriends(UserId(1))
      verify(userCache).fetchUserFriends("some-token")
      verify(userCache).saveUserFriends(UserId(1), ids, "some-token")
    }
  }

  "fetchUserInfo" should "fetch info if the id is valid" in {
    val userCache = mock[AbstractUserCache]
    val gh = mock[GithubApi]
    val info = UserInfo(UserId(1), "username")

    when(userCache.fetchUserInfo(UserId(1), None)).thenCallRealMethod()
    when(userCache.githubApi).thenReturn(gh)
    when(gh.getUserInfo(UserId(1), None)).thenReturnFuture(info)

    userCache.fetchUserInfo(UserId(1), None).futureValue shouldBe info
  }

  it should "fail if the id is invalid" in {
    val userCache = mock[AbstractUserCache]
    val gh = mock[GithubApi]

    when(userCache.fetchUserInfo(UserId(1), None)).thenCallRealMethod()
    when(userCache.githubApi).thenReturn(gh)
    when(gh.getUserInfo(UserId(1), None)).thenReturn(Future.failed(new Exception("User doestn't exist")))

    userCache.fetchUserInfo(UserId(1), None).failed.futureValue shouldBe an[Exception]
  }

  "fetchCurrentUserInfo" should "fetch info if the token is valid" in {
    val userCache = mock[AbstractUserCache]
    val gh = mock[GithubApi]
    val info = UserInfo(UserId(1), "username")

    when(userCache.fetchCurrentUserInfo("some-token")).thenCallRealMethod()
    when(userCache.githubApi).thenReturn(gh)
    when(gh.getCurrentUserInfo("some-token")).thenReturnFuture(info)

    userCache.fetchCurrentUserInfo("some-token").futureValue shouldBe info
  }

  it should "fail if the token is invalid" in {
    val userCache = mock[AbstractUserCache]
    val gh = mock[GithubApi]

    when(userCache.fetchCurrentUserInfo("bad-token")).thenCallRealMethod()
    when(userCache.githubApi).thenReturn(gh)
    when(gh.getCurrentUserInfo("bad-token")).thenReturn(Future.failed(new Exception("Invalid token")))

    userCache.fetchCurrentUserInfo("bad-token").failed.futureValue shouldBe an[Exception]
  }

  "fetchUserFriends" should "fetch friends if the token is valid" in {
    val userCache = mock[AbstractUserCache]
    val gh = mock[GithubApi]
    val ids = Set(2, 3, 4).map(UserId(_))

    when(userCache.fetchUserFriends("some-token")).thenCallRealMethod()
    when(userCache.githubApi).thenReturn(gh)
    when(gh.getCurrentUserFollowing("some-token")).thenReturnFuture(ids)

    userCache.fetchUserFriends("some-token").futureValue should contain theSameElementsAs ids
  }

  it should "fail if the token is invalid" in {
    val userCache = mock[AbstractUserCache]
    val gh = mock[GithubApi]

    when(userCache.fetchUserFriends("bad-token")).thenCallRealMethod()
    when(userCache.githubApi).thenReturn(gh)
    when(gh.getCurrentUserFollowing("bad-token")).thenReturn(Future.failed(new Exception("Invalid token")))

    userCache.fetchUserFriends("bad-token").failed.futureValue shouldBe an[Exception]
  }
}
