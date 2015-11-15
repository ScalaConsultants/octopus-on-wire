package services

import play.api.libs.json.JsValue

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

object UserCache {

  private val tokenCache = TrieMap[String, UserId]()
  private val userCache = TrieMap[UserId, UserInfo]()
  private val userFriends = TrieMap[UserId, Seq[UserId]]()

  def getOrFetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]] =
    userCache.get(id) match {
      case None => fetchUserInfo(id, tokenOpt)
      case someInfo => Future.successful(someInfo)
    }

  def getOrFetchUserId(tokenOpt: Option[String]): Future[Option[UserId]] =
    tokenOpt.map { token =>
      tokenCache.get(token) match {
        case None => fetchAndSaveUserId(token)
        case someId => Future.successful(someId)
      }
    }.getOrElse(Future(None))

  def getOrFetchUserFriends(tokenOpt: Option[String]): Future[Seq[UserId]] =
    tokenOpt.map(token => tokenCache.get(token).flatMap(userFriends.get) match {
      case None => fetchUserFriends(token)
      case Some(friends) => Future.successful(friends)
    }).getOrElse(Future(Nil))

  def fetchUserFriends(token: String): Future[Seq[UserId]] =
    GithubApi.getCurrentUserFollowing(token).map { json =>
      val friends = json.result.asOpt[Seq[JsValue]].toList.flatten
        .flatMap(friend => (friend \ "id").asOpt[Long]).map(UserId)

      //update cache
      tokenCache.get(token).foreach(userFriends(_) = friends)

      friends
    }

  private def fetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]] = {
    GithubApi.getUserInfo(id, tokenOpt).map { result =>
      val loginOpt = (result \ "login").asOpt[String]
      val userOpt = loginOpt.map(name => UserInfo(id, name))

      //update cache
      userOpt.foreach(userCache(id) = _)

      userOpt
    }
  }

  private def fetchAndSaveUserId(token: String): Future[Option[UserId]] = {
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