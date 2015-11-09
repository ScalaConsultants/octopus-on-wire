package services

import scala.concurrent.ExecutionContext.Implicits.global
import tools.JsLookupResultOps._

import scala.collection.concurrent.TrieMap
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scalac.octopusonwire.shared.domain.{UserInfo, UserId}

object UserCache {

  private val tokenCache = TrieMap[String, UserId]()
  private val userCache = TrieMap[UserId, UserInfo]()

  def getOrFetchUserInfo(id: UserId): Future[Option[UserInfo]] =
    userCache.get(id) match{
      case None => fetchUserInfo(id)
      case res => Future.successful(res)
    }

  def getOrFetchUserId(token: String): Option[UserId] = {
    val cached = tokenCache.get(token)

    //we need to update user login anyways
    if (cached.isDefined) Future(fetchUserId(token).map(info => fetchUserInfo(info)))

    cached.orElse(fetchUserId(token))
  }

  private def fetchUserInfo(id: UserId): Future[Option[UserInfo]] = {
    GithubApi.getUserInfo(id).map { result =>
      val loginOpt = (result \ "login").toOptionString
      val userOpt = loginOpt.map(name => UserInfo(id, name))

      //update cache
      userOpt.foreach(userCache(id) = _)

      userOpt
    }
  }

  private def fetchUserId(token: String): Option[UserId] = {
    val result = Await.result(
      awaitable = GithubApi.getCurrentUserInfo(token),
      atMost = Duration.Inf
    )

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