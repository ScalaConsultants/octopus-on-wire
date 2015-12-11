package services

import play.api.libs.json.JsValue

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

abstract class UserCache{
  def getOrFetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]]
  def getOrFetchUserId(tokenOpt: Option[String]): Future[Option[UserId]]
  def getOrFetchUserFriends(tokenOpt: Option[String]): Future[Seq[UserId]]
  def fetchUserFriends(token: String): Future[Seq[UserId]]
  protected def fetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]]
  protected def fetchAndSaveUserId(token: String): Future[Option[UserId]]
}

object InMemoryUserCache extends UserCache{

  private val tokenCache = TrieMap[String, UserId]()
  private val userCache = TrieMap[UserId, UserInfo]()
  private val userFriends = TrieMap[UserId, Seq[UserId]]()

  override def getOrFetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]] =
    userCache.get(id) match {
      case None => fetchUserInfo(id, tokenOpt).map{infoOpt =>
        infoOpt.foreach(userCache(id) = _)
        infoOpt
      }
      case someInfo => Future.successful(someInfo)
    }

  override def getOrFetchUserId(tokenOpt: Option[String]): Future[Option[UserId]] =
    tokenOpt.map { token =>
      tokenCache.get(token) match {
        case None => fetchAndSaveUserId(token)
        case someId => Future.successful(someId)
      }
    }.getOrElse(Future(None))

  override def getOrFetchUserFriends(tokenOpt: Option[String]): Future[Seq[UserId]] =
    tokenOpt.map(token => tokenCache.get(token).flatMap(userFriends.get) match {
      case None =>
        fetchUserFriends(token).map{friends =>
          tokenCache.get(token).foreach(userFriends(_) = friends)
          friends
        }
      case Some(friends) => Future.successful(friends)
    }).getOrElse(Future(Nil))

  override def fetchUserFriends(token: String): Future[Seq[UserId]] =
    GithubApi.getCurrentUserFollowing(token).map {_.result.asOpt[Seq[JsValue]].toList.flatten
      .flatMap(friend => (friend \ "id").asOpt[Long]).map(UserId)
    }

  override protected def fetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]] = {
    GithubApi.getUserInfo(id, tokenOpt).map { result =>
      val loginOpt = (result \ "login").asOpt[String]
      loginOpt.map(name => UserInfo(id, name))
    }
  }

  override protected def fetchAndSaveUserId(token: String): Future[Option[UserId]] = {
    GithubApi.getCurrentUserInfo(token)
      .map { result =>
        val uid = (result \ "id").asOpt[Long].map(UserId)
        val ulogin = (result \ "login").asOpt[String]

        val userOption = (uid, ulogin).zipped.map((id, name) => UserInfo(id, name)).headOption

        //update caches
        userOption.foreach(user => {
          userCache(user.userId) = user
          tokenCache(token) = user.userId
        })

        uid
      }
  }
}