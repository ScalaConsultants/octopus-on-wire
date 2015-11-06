package services

import config.Github
import config.Github._
import domain.UserId
import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import play.api.mvc.Results.EmptyContent
import play.mvc.Http.HeaderNames
import tools.JsLookupResultOps._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GithubApi {
  private def buildCall(url: String, token: String) = WS.url(url)
    .withRequestTimeout(ApiRequestTimeout)
    .withQueryString(AccessTokenKey -> token)

  def getUserInfo(token: String): Future[JsValue] =
    buildCall(UserUrl, token).get().map(_.json)

  def getUserId(tokenOption: Option[String]): Option[UserId] =
    tokenOption.flatMap(token => UserCache.getOrFetchUserId(token))

  def getGithubToken(code: String): Future[Option[String]] = {
    val result = WS.url(Github.AccessTokenUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withHeaders(HeaderNames.ACCEPT -> "application/json")
      .withQueryString("client_id" -> ClientId, "client_secret" -> ClientSecret, "code" -> code)
      .post(EmptyContent())

    result.map(r => (r.json \ AccessTokenKey).toOptionString)
  }
}