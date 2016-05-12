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

  override def getUserInfo(id: UserId): Future[Option[UserInfo]] = userDao.userById(id)

  override def saveUserInfo(userInfo: UserInfo): Future[Int] = userDao.saveUserInfo(userInfo)

  override def getUserIdByToken(token: String): Future[Option[UserId]] = tokens.userIdByToken(token)

  override def saveUserToken(token: String, userId: UserId): Future[Int] = tokens.saveUserToken(token, userId)

  override def getUserFriends(userId: UserId): Future[Option[Set[UserId]]] =
    userFriendPairsDao.getUserFriends(userId).map {
      case friends if friends.nonEmpty => Some(friends)
      case _ => None
    }

  override def saveUserFriends(userId: UserId, friends: Set[UserId], token: String): Future[Unit] = {
    val saveUsersFuture = Future.sequence(friends.map(getOrFetchUserInfo(_, Some(token))))

    for {
      users <- saveUsersFuture
    } yield userFriendPairsDao.saveUserFriends(userId, friends)
  }
}
