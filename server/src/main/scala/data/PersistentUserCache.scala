package data

import com.google.inject.Inject
import domain._
import services.GithubApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class PersistentUserCache @Inject()(tokens: TokenPairDao,
                                    trustedUsersDao: TrustedUserDao,
                                    userFriendPairsDao: UserFriendPairDao,
                                    userDao: UserDao,
                                    gh: GithubApi) extends UserCache {

  override def githubApi: GithubApi = gh

  override def isUserTrusted(id: UserId): Future[Boolean] = trustedUsersDao.isUserTrusted(id)

  override def getUserInfo(id: UserId): Future[UserInfo] = userDao.userById(id)

  override def saveUserInfo(userInfo: UserInfo): Unit = userDao.saveUserInfo(userInfo)

  override def getUserIdByToken(token: String): Future[UserId] = tokens.userIdByToken(token)

  override def saveUserToken(token: String, user: UserInfo): Unit = tokens.saveUserToken(token, user)

  override def getUserFriends(userId: UserId): Future[Set[UserId]] =
    userFriendPairsDao.getUserFriends(userId).filter(_.nonEmpty)

  override def saveUserFriends(userId: UserId, friends: Set[UserId], githubTokenOpt: Option[String]): Unit = {
    val saveUsersFuture = Future.sequence(friends.map(getOrFetchUserInfo(_, githubTokenOpt)))

    for {
      users <- saveUsersFuture
    } yield userFriendPairsDao.saveUserFriends(userId, friends)
  }
}
