package services

import tools.JsLookupResultOps._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

object UserCache {

  private val tokenCache = TrieMap[String, UserId]()
  private val userCache = TrieMap[UserId, UserInfo]()

  def getOrFetchUserInfo(id: UserId): Future[Option[UserInfo]] =
    userCache.get(id) match {
      case None => fetchUserInfo(id)
      case someInfo => Future.successful(someInfo)
    }

  def getOrFetchUserId(tokenOpt: Option[String]): Future[Option[UserId]] =
    tokenOpt.map { token =>
      tokenCache.get(token) match {
        case None => fetchAndSaveUserId(token)
        case someId => Future.successful(someId)
      }
    }.getOrElse(Future(None))

  private def fetchUserInfo(id: UserId): Future[Option[UserInfo]] = {
    GithubApi.getUserInfo(id).map { result =>
      val loginOpt = (result \ "login").toOptionString
      val userOpt = loginOpt.map(name => UserInfo(id, name))

      //update cache
      userOpt.foreach(userCache(id) = _)

      userOpt
    }
  }

  private def fetchAndSaveUserId(token: String): Future[Option[UserId]] = {
    GithubApi.getCurrentUserInfo(token)
      .map { result =>
        val uid = (result \ "id").toOptionLong.map(UserId)
        val ulogin = (result \ "login").toOptionString

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