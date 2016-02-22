package data

import play.api.libs.json.JsValue
import services.GithubApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserInfo, UserId}

trait UserCache {
  def isUserTrusted(id: UserId): Future[Boolean]

  def getUserInfo(id: UserId): Future[Option[UserInfo]]

  def saveUserInfo(userInfo: UserInfo): Unit

  def getUserIdByToken(token: String): Future[Option[UserId]]

  def saveUserToken(token: String, user: UserInfo): Unit

  def getUserFriends(userId: UserId): Future[Option[Set[UserId]]]

  def saveUserFriends(userId: UserId, friends: Set[UserId], tokenOpt: Option[String]): Unit

  def getOrFetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]] =
    getUserInfo(id).flatMap {
      case None => fetchUserInfo(id, tokenOpt).map { infoOpt =>
        infoOpt foreach saveUserInfo
        infoOpt
      }
      case someInfo => Future.successful(someInfo)
    }

  def getOrFetchUserId(tokenOpt: Option[String]): Future[Option[UserId]] =
    tokenOpt.map { token =>
      getUserIdByToken(token).flatMap {
        case None => fetchCurrentUserInfo(token).map { infoOpt =>
          //update caches
          infoOpt.foreach(user => {
            saveUserInfo(user)
            saveUserToken(token, user)
          })

          infoOpt.map(_.userId)
        }
        case someId => Future.successful(someId)
      }
    }.getOrElse(Future(None))

  def getOrFetchUserFriends(token: String, id: UserId): Future[Set[UserId]] = {
    val dbFriends = for {
      Some(friends) <- getUserFriends(id)
    } yield friends

    dbFriends.fallbackTo {
      fetchUserFriends(token).map { friends =>
        saveUserFriends(id, friends, Some(token))
        friends
      }
    }
  }

  protected def fetchUserInfo(id: UserId, tokenOpt: Option[String]): Future[Option[UserInfo]] = {
    GithubApi.getUserInfo(id, tokenOpt).map { result =>
      val loginOpt = (result \ "login").asOpt[String]
      loginOpt.map(name => UserInfo(id, name))
    }
  }

  protected def fetchCurrentUserInfo(token: String): Future[Option[UserInfo]] = {
    GithubApi.getCurrentUserInfo(token)
      .map { result =>
        val uid = (result \ "id").asOpt[Long].map(UserId)
        val ulogin = (result \ "login").asOpt[String]

        (uid, ulogin).zipped.map((id, name) => UserInfo(id, name)).headOption
      }
  }

  def fetchUserFriends(token: String): Future[Set[UserId]] =
    GithubApi.getCurrentUserFollowing(token).map {
      _.result.asOpt[Seq[JsValue]].toList.flatten
        .flatMap(friend => (friend \ "id").asOpt[Long]).map(UserId).toSet
    }
}
