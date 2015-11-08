package services

import tools.JsLookupResultOps._

import scala.collection.concurrent.TrieMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalac.octopusonwire.shared.domain.UserId

object UserCache {

  //temporary cache
  val cache = TrieMap[String, UserId]()

  def getOrFetchUserId(token: String): Option[UserId] = cache.get(token).orElse(fetchUserId(token))

  private def fetchUserId(token: String): Option[UserId] = {
    val result = Await.result(
      awaitable = GithubApi.getUserInfo(token),
      atMost = Duration.Inf
    )

    val uid = (result \ "id").toOptionLong.map(UserId)

    //update cache
    uid.foreach(cache(token) = _)

    uid
  }
}
