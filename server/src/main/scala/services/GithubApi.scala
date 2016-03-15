package services

import com.google.inject.Inject
import config.Github
import config.Github._
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WS, WSRequest}
import play.api.mvc.Results.EmptyContent
import play.mvc.Http.HeaderNames

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.UserId

class GithubApi @Inject() (ws: WSClient){
  private def buildCall(url: String): WSRequest =
    ws.url(url).withRequestTimeout(ApiRequestTimeout)

  private def buildUserCall(url: String, token: String): WSRequest =
    buildCall(url).withQueryString(AccessTokenKey -> token)

  private def buildOptionalUserCall(url: String, tokenOpt: Option[String]) =
    tokenOpt.map(buildUserCall(url, _)).getOrElse(buildCall(url))

  def getCurrentUserInfo(token: String): Future[JsValue] =
    buildUserCall(UserUrl, token).get().map(_.json)

  def getUserInfo(userId: UserId, tokenOpt: Option[String]): Future[JsValue] =
    buildOptionalUserCall(s"$UserUrl/${userId.value}", tokenOpt).get().map(_.json)

  def getCurrentUserFollowing(token: String): Future[JsValue] =
    buildUserCall(UserFollowingUrl, token).get().map(_.json)

  def getGithubToken(code: String): Future[String] = {
    val result = ws.url(Github.AccessTokenUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withHeaders(HeaderNames.ACCEPT -> "application/json")
      .withQueryString("client_id" -> ClientId, "client_secret" -> ClientSecret, "code" -> code)
      .post(EmptyContent())

    result.map(r => (r.json \ AccessTokenKey).asOpt[String]).flatMap {
      case Some(token) => Future.successful(token)
      case _ => Future.failed(new Exception("Invalid code"))
    }
  }
}