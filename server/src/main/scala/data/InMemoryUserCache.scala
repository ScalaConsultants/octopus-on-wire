package data

import com.google.inject.Inject
import services.GithubApi

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class InMemoryUserCache @Inject() (gh: GithubApi) extends UserCache {
  private val tokenCache = TrieMap[String, UserId]()
  private val userCache = TrieMap[UserId, UserInfo]()

  private val userFriends = TrieMap[UserId, Set[UserId]]()

  val trustedUsers: mutable.Set[UserId] = mutable.Set.empty

  override def isUserTrusted(id: UserId): Future[Boolean] = Future {
    trustedUsers contains id
  }

  override def getUserInfo(id: UserId): Future[UserInfo] = Future {
    userCache.get(id)
  } flatMap {
    case Some(info) => Future.successful(info)
    case None => Future.failed(new Exception("User info not found"))
  }

  override def saveUserInfo(userInfo: UserInfo): Unit = {
    userCache(userInfo.userId) = userInfo
  }

  override def getUserIdByToken(token: String): Future[UserId] = Future {
    tokenCache.get(token)
  } flatMap {
    case Some(info) => Future.successful(info)
    case None => Future.failed(new Exception("User info not found"))
  }

  override def saveUserToken(token: String, user: UserInfo): Unit = {
    tokenCache(token) = user.userId
  }

  override def getUserFriends(userId: UserId): Future[Set[UserId]] = Future {
    userFriends.getOrElse(userId, Set.empty)
  }.flatMap {
    case friends if friends.nonEmpty => Future.successful(friends)
    case _ => Future.failed(new Exception("No friends found"))
  }

  override def saveUserFriends(userId: UserId, friends: Set[UserId], tokenOpt: Option[String]): Unit = {
    userFriends(userId) = friends
  }

  override def githubApi: GithubApi = gh
}