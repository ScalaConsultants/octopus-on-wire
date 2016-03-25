package services

import config.Github._
import io.scalac.octopus.server.OctoSpec
import org.mockito.Mockito._
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Results.EmptyContent
import play.mvc.Http.HeaderNames

import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class GithubApiTests extends OctoSpec {
  "getCurrentUserInfo" should "get current user info" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]
    when(ws.url(UserUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withQueryString(AccessTokenKey -> "valid-token")
      .get()
    ).thenReturnFuture(response)

    when(response.json).thenReturn(JsObject(
      Map("id" -> JsNumber(1), "login" -> JsString("username"))
    ))

    val timeout = Timeout(1.second)

    val gh = new GithubApi(ws)
    gh.getCurrentUserInfo("valid-token").futureValue(timeout) shouldBe UserInfo(UserId(1), "username")
  }

  it should "fail if the token is invalid" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]

    when(ws.url(UserUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withQueryString(AccessTokenKey -> "bad-token")
      .get()
    ).thenReturnFuture(response)

    when(response.json).thenReturn(JsObject(
      Map("message" -> JsString("Bad credentials"))
    ))

    val timeout = Timeout(1.second)

    val gh = new GithubApi(ws)
    gh.getCurrentUserInfo("bad-token").failed.futureValue(timeout) shouldBe an[Exception]
  }

  "getUserInfo" should "get user info if user exists" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]

    val userId = UserId(1)

    when(ws.url(s"$UserUrl/${userId.value}")
      .withRequestTimeout(ApiRequestTimeout)
      .get()
    ).thenReturnFuture(response)

    when(response.json).thenReturn(JsObject(
      Map("login" -> JsString("username"))
    ))

    val gh = new GithubApi(ws)

    gh.getUserInfo(userId, None).futureValue shouldBe UserInfo(userId, "username")
  }

  it should "fail when given nonexistent id" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]

    val userId = UserId(1)

    when(ws.url(s"$UserUrl/${userId.value}")
      .withRequestTimeout(ApiRequestTimeout)
      .get()
    ).thenReturnFuture(response)

    when(response.json).thenReturn(JsObject(
      Map("message" -> JsString("Not found"))
    ))

    val gh = new GithubApi(ws)

    gh.getUserInfo(userId, None).failed.futureValue shouldBe an[Exception]
  }

  "getCurrentUserFollowing" should "get some users for real token" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]

    val token = "valid-token"

    when(ws.url(UserFollowingUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withQueryString(AccessTokenKey -> token)
      .get()
    ).thenReturnFuture(response)

    when(response.json).thenReturn(JsArray(
      List(
        1, 2, 3
      ).map(id => JsObject(Map("id" -> JsNumber(id))))
    ))

    val gh = new GithubApi(ws)

    gh.getCurrentUserFollowing(token).futureValue should contain theSameElementsAs Set(1, 2, 3).map(UserId(_))
  }

  it should "fail when given invalid token" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]

    val token = "bad-token"

    when(ws.url(UserFollowingUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withQueryString(AccessTokenKey -> token)
      .get()
    ).thenReturnFuture(response)

    when(response.json).thenReturn(
      JsObject(
        Map(
          "error" -> JsString("Bad credentials")
        )
      )
    )

    val gh = new GithubApi(ws)

    gh.getCurrentUserFollowing(token).failed.futureValue shouldBe an[Exception]
  }

  "getGithubToken" should "get a token when given valid code" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]

    val theCode = "valid-code"
    val expectedToken = "valid-token"

    when(ws.url(AccessTokenUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withHeaders(HeaderNames.ACCEPT -> "application/json")
      .withQueryString("client_id" -> ClientId, "client_secret" -> ClientSecret, "code" -> theCode)
      .post(EmptyContent())
    ).thenReturnFuture(response)

    when(response.json).thenReturn(JsObject(Map(AccessTokenKey -> JsString(expectedToken))))

    val gh = new GithubApi(ws)
    gh.getGithubToken(theCode).futureValue shouldBe expectedToken
  }

  it should "fail when given bad code" in {
    val ws = mockDeep[WSClient]
    val response = mock[WSResponse]

    val theCode = "bad-code"

    when(ws.url(AccessTokenUrl)
      .withRequestTimeout(ApiRequestTimeout)
      .withHeaders(HeaderNames.ACCEPT -> "application/json")
      .withQueryString("client_id" -> ClientId, "client_secret" -> ClientSecret, "code" -> theCode)
      .post(EmptyContent())
    ).thenReturnFuture(response)

    when(response.json).thenReturn(JsObject(Map("error" -> JsString("bad_verification_code"))))

    val gh = new GithubApi(ws)
    gh.getGithubToken(theCode).failed.futureValue shouldBe an[Exception]
  }
}