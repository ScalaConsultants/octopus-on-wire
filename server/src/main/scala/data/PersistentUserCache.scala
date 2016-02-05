package data

import domain.{TokenPairs, UserFriendPairs, Users}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class PersistentUserCache extends UserCache {

  override def getUserInfo(id: UserId): Future[Option[UserInfo]] = Users.userById(id)

  override def saveUserInfo(userInfo: UserInfo): Unit = Users.saveUserInfo(userInfo)

  override def getUserIdByToken(token: String): Future[Option[UserId]] = TokenPairs.userIdByToken(token)

  override def saveUserToken(token: String, user: UserInfo): Unit = TokenPairs.saveUserToken(token, user)

  override def getUserFriends(userId: UserId): Future[Option[Set[UserId]]] = UserFriendPairs.getUserFriends(userId)

  override def saveUserFriends(userId: UserId, friends: Set[UserId], githubTokenOpt: Option[String]): Unit = {
    val saveUsersFuture = Future.sequence(friends.map(getOrFetchUserInfo(_, githubTokenOpt)))

    for {
      users <- saveUsersFuture
    } yield UserFriendPairs.saveUserFriends(userId, friends)
  }
}
