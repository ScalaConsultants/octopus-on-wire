package data

import play.api.libs.json.JsValue
import services.GithubApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserInfo, UserId}

trait UserCache {
  def isUserTrusted(id: UserId): Future[Boolean]

  def getUserInfo(id: UserId): Future[UserInfo]

  def saveUserInfo(userInfo: UserInfo): Unit

  def getUserIdByToken(token: String): Future[UserId]

  def saveUserToken(token: String, user: UserInfo): Unit

  def getUserFriends(userId: UserId): Future[Set[UserId]]

  def saveUserFriends(userId: UserId, friends: Set[UserId], tokenOpt: Option[String]): Unit

  def getOrFetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[UserInfo] =
    getUserInfo(id).recoverWith { case _ =>
      fetchUserInfo(id, tokenOpt).map { info =>
        saveUserInfo(info)
        info
      }
    }

  def getOrFetchUserId(token: String): Future[UserId] =
    getUserIdByToken(token).recoverWith {
      case _ => fetchCurrentUserInfo(token).flatMap { info =>
        //update caches
        saveUserInfo(info)
        saveUserToken(token, info)
        Future.successful(info.userId)
      }.recoverWith {
        case _ => Future.failed(new Exception("Invalid user token"))
      }
    }

  def getOrFetchUserFriends(token: String, id: UserId): Future[Set[UserId]] = {
    val dbFriends = getUserFriends(id)

    dbFriends.fallbackTo {
      fetchUserFriends(token).map { friends =>
        saveUserFriends(id, friends, Some(token))
        friends
      }
    }
  }

  protected def fetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[UserInfo] = {
    GithubApi.getUserInfo(id, tokenOpt).map { result =>
      val loginOpt = (result \ "login").asOpt[String]
      loginOpt.map(name => UserInfo(id, name))
    }.flatMap {
      case Some(info) => Future.successful(info)
      case None => Future.failed(new Exception("User info not found"))
    }
  }

  protected def fetchCurrentUserInfo(token: String): Future[UserInfo] = {
    GithubApi.getCurrentUserInfo(token)
      .flatMap { result =>
        val uid = (result \ "id").asOpt[Long].map(UserId)
        val ulogin = (result \ "login").asOpt[String]

        (uid zip ulogin).headOption match {
          case Some((id, name)) => Future.successful(UserInfo(id, name))
          case _ => Future.failed(new Exception("Invalid token"))
        }
      }
  }

  def fetchUserFriends(token: String): Future[Set[UserId]] =
    GithubApi.getCurrentUserFollowing(token).map {
      _.result.asOpt[Seq[JsValue]].toList.flatten
        .flatMap(friend => (friend \ "id").asOpt[Long]).map(UserId).toSet
    }
}
