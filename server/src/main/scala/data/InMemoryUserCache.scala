package data

import com.google.inject.Inject
import services.GithubApi

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class InMemoryUserCache @Inject()(gh: GithubApi) extends UserCache {
  private val tokenCache = TrieMap[String, UserId]()
  private val userCache = TrieMap[UserId, UserInfo]()

  private val userFriends = TrieMap[UserId, Set[UserId]]()

  val trustedUsers: mutable.Set[UserId] = mutable.Set.empty

  override def isUserTrusted(id: UserId): Future[Boolean] = Future {
    trustedUsers contains id
  }

  override def getUserInfo(id: UserId): Future[Option[UserInfo]] = Future {
    userCache.get(id)
  }

  override def saveUserInfo(userInfo: UserInfo): Future[Int] = Future.successful {
    val result = userCache.keySet.contains(userInfo.userId)
    userCache(userInfo.userId) = userInfo

    if (result) 0 else 1
  }

  override def getUserIdByToken(token: String): Future[Option[UserId]] = Future {
    tokenCache.get(token)
  }

  override def saveUserToken(token: String, userId: UserId): Future[Int] = Future {
    val result = tokenCache.keySet.contains(token)
    tokenCache(token) = userId

    if (result) 0 else 1
  }

  override def getUserFriends(userId: UserId): Future[Option[Set[UserId]]] = Future {
    userFriends.getOrElse(userId, Set.empty)
  }.map {
    friends => Some(friends).filter(_.nonEmpty)
  }

  override def saveUserFriends(userId: UserId, friends: Set[UserId], token: String): Future[Unit] = Future {
    userFriends(userId) = friends
  }

  override def githubApi: GithubApi = gh
}