package io.scalac.octopus.server.controllers

import config.Github.AccessTokenKey
import config.ServerConfig
import controllers.Application
import data.{PersistentEventSource, PersistentUserCache}
import domain.EventDao
import io.scalac.octopus.server.OctoSpec
import org.mockito.Mockito._
import play.api.Environment
import play.api.mvc.{Cookie, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.GithubApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.JoinSuccessful
import scalac.octopusonwire.shared.domain.{Event, EventId, UserId}

class ApplicationTests extends OctoSpec {
  private def boilerplate = (
    mock[PersistentEventSource],
    mock[PersistentUserCache],
    mock[GithubApi],
    mock[Environment],
    mock[EventDao]
    )

  "loginWithGithub" should "redirect to source with token if code is valid" in {
    val code = "valid-code"
    val token = "some-token"
    val redirectTo = "http://some-website.com"

    val (es, uc, gh, env, edao) = boilerplate

    when(gh.getGithubToken(code)).thenReturnFuture(token)
    when(uc.getOrFetchUserId(token)).thenReturnFuture(UserId(1))

    val app = new Application(es, uc, gh, env, edao)

    val result = app.loginWithGithub(code, redirectTo)(FakeRequest())

    cookies(result).get(AccessTokenKey) should contain(Cookie(name = AccessTokenKey,
      value = token,
      maxAge = Some(14.days.toMillis.toInt),
      domain = Some(ServerConfig.CookieDomain),
      secure = false,
      httpOnly = true)
    )

    redirectLocation(result) should contain(redirectTo)

    whenReady(result) { _ =>
      verify(uc).getOrFetchUserId(token)
    }
  }

  it should "fail if code is invalid" in {
    val code = "bad-code"
    val redirectTo = "http://some-website.com"

    val (es, uc, gh, env, edao) = boilerplate

    when(gh.getGithubToken(code)).thenReturn(Future.failed(new Exception("Invalid code")))

    val app = new Application(es, uc, gh, env, edao)

    app.loginWithGithub(code, redirectTo)(FakeRequest())
      .failed.futureValue shouldBe an[Exception]
  }

  "joinEventWithGithub" should "call EventSource.joinEvent when given good code" in {
    val code = "valid-code"
    val token = "some-token"
    val redirectTo = "http://some-website.com"

    val now = System.currentTimeMillis

    val expectedEvent =
      Event(EventId(1), "some event", now + 1.hour.toMillis, now + 1.day.toMillis, 0, "somewhere", "example.com")

    val (es, uc, gh, env, edao) = boilerplate

    when(gh.getGithubToken(code)).thenReturnFuture(token)
    when(uc.getOrFetchUserId(token)).thenReturnFuture(UserId(1))

    when(es.eventById(expectedEvent.id)).thenReturnFuture(expectedEvent)
    when(es.joinEvent(UserId(1), expectedEvent.id)).thenReturnFuture(JoinSuccessful.apply)

    val app = new Application(es, uc, gh, env, edao)

    val result = app.joinEventWithGithub(1, code, redirectTo)(FakeRequest())

    whenReady(result) { _ =>
      verify(es).joinEvent(UserId(1), expectedEvent.id)
      verify(uc).getOrFetchUserId(token)
    }

    cookies(result).get(AccessTokenKey) should contain(Cookie(name = AccessTokenKey,
      value = token,
      maxAge = Some(14.days.toMillis.toInt),
      domain = Some(ServerConfig.CookieDomain),
      secure = false,
      httpOnly = true)
    )

    redirectLocation(result) should contain(redirectTo)
  }

  it should "fail miserably when given bad code" in {
    val code = "bad-code"
    val redirectTo = "http://some-website.com"

    val (es, uc, gh, env, edao) = boilerplate

    when(gh.getGithubToken(code)).thenReturn(Future.failed(new Exception("Invalid code")))

    val app = new Application(es, uc, gh, env, edao)

    app.joinEventWithGithub(1, code, redirectTo)(FakeRequest())
      .failed.futureValue shouldBe an[Exception]
  }

  "flagEventWithGithub" should "call something when given good code" in {
    val code = "valid-code"
    val token = "valid-token"
    val redirectTo = "http://some-website.com"

    val (es, uc, gh, env, edao) = boilerplate

    when(gh.getGithubToken(code)).thenReturnFuture(token)
    when(uc.getOrFetchUserId(token)).thenReturnFuture(UserId(1))
    when(es.addFlag(EventId(1), UserId(1))).thenReturnFuture(true)

    val app = new Application(es, uc, gh, env, edao)

    val result = app.flagEventWithGithub(1, code, redirectTo)(FakeRequest())

    whenReady(result) { _ =>
      verify(es).addFlag(EventId(1), UserId(1))
      verify(uc).getOrFetchUserId(token)
    }

    cookies(result).get(AccessTokenKey) should contain(Cookie(name = AccessTokenKey,
      value = token,
      maxAge = Some(14.days.toMillis.toInt),
      domain = Some(ServerConfig.CookieDomain),
      secure = false,
      httpOnly = true)
    )

    redirectLocation(result) should contain(redirectTo)
  }

  it should "fail when given bad code" in {
    val code = "bad-code"
    val redirectTo = "http://some-website.com"

    val (es, uc, gh, env, edao) = boilerplate

    when(gh.getGithubToken(code)).thenReturn(Future.failed(new Exception("Invalid code")))

    val app = new Application(es, uc, gh, env, edao)

    app.flagEventWithGithub(1, code, redirectTo)(FakeRequest())
      .failed.futureValue shouldBe an[Exception]
  }
}
