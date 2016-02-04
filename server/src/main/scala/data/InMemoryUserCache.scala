package data

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserInfo, UserId}

class InMemoryUserCache extends UserCache {
  private val tokenCache = TrieMap[String, UserId]()
  private val userCache = TrieMap[UserId, UserInfo]()

  private val userFriends = TrieMap[UserId, Set[UserId]]()

  override def getUserInfo(id: UserId): Future[Option[UserInfo]] = Future {
    userCache.get(id)
  }

  override def saveUserInfo(userInfo: UserInfo): Unit = {
    userCache(userInfo.userId) = userInfo
  }

  override def getUserIdByToken(token: String): Future[Option[UserId]] = Future {
    tokenCache.get(token)
  }

  override def saveUserToken(token: String, user: UserInfo): Unit = {
    tokenCache(token) = user.userId
  }

  override def getUserFriends(userId: UserId): Future[Option[Set[UserId]]] = Future {
    userFriends.get(userId)
  }

  override def saveUserFriends(userId: UserId, friends: Set[UserId], tokenOpt: Option[String]): Unit = {
    userFriends(userId) = friends
  }
}