package data

import domain.UserIdentity
import play.api.libs.json.JsValue
import services.GithubApi

import scala.concurrent.{ExecutionContext, Future}
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

trait UserCache {
  def githubApi: GithubApi

  def isUserTrusted(id: UserId): Future[Boolean]

  def getUserInfo(id: UserId): Future[UserInfo]

  def saveUserInfo(userInfo: UserInfo): Future[Int]

  def getUserIdByToken(token: String): Future[UserId]

  def saveUserToken(token: String, userId: UserId): Future[Int]

  def getUserFriends(userId: UserId): Future[Set[UserId]]

  def saveUserFriends(userId: UserId, friends: Set[UserId], token: String): Future[Unit]

  def getOrFetchUserInfo(id: UserId, tokenOpt: Option[String])(implicit ec: ExecutionContext): Future[UserInfo] =
    getUserInfo(id).recoverWith { case _ =>
      fetchUserInfo(id, tokenOpt).map { info =>
        saveUserInfo(info)
        info
      }
    }

  def getOrFetchUserId(token: String)(implicit ec: ExecutionContext): Future[UserId] =
    getUserIdByToken(token).recoverWith {
      case _ => fetchCurrentUserInfo(token).map { info =>
        //update caches
        saveUserInfo(info)
        saveUserToken(token, info.userId)
        info.userId
      }
    }

  def getOrFetchUserFriends(userIdentity: UserIdentity)(implicit ec: ExecutionContext): Future[Set[UserId]] = {
    val UserIdentity(token, id) = userIdentity
    val dbFriends = getUserFriends(id)

    dbFriends.recoverWith { case _ =>
      fetchUserFriends(token).map { friends =>
        saveUserFriends(id, friends, token)
        friends
      }
    }
  }

  def fetchUserInfo(id: UserId, tokenOpt: Option[String])(implicit ec: ExecutionContext): Future[UserInfo] =
    githubApi.getUserInfo(id, tokenOpt)

  def fetchCurrentUserInfo(token: String)(implicit ec: ExecutionContext): Future[UserInfo] =
    githubApi.getCurrentUserInfo(token)

  def fetchUserFriends(token: String)(implicit ec: ExecutionContext): Future[Set[UserId]] =
    githubApi.getCurrentUserFollowing(token)
}
