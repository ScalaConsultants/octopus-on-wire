package controllers

import java.nio.ByteBuffer

import boopickle.Default._
import com.google.common.net.MediaType
import config.Github._
import config.{Github, Router, ServerConfig}
import data.{PersistentEventSource, EventSource, PersistentUserCache, UserCache}
import play.api.mvc._
import services.{ApiService, GithubApi}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.EventId

object Application extends Controller {
  val eventSource: EventSource = new PersistentEventSource
  val userCache: UserCache = new PersistentUserCache

  def CorsEnabled(result: Result)(implicit request: Request[Any]): Result =
    result.withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers(ORIGIN),
      ACCESS_CONTROL_ALLOW_HEADERS -> CONTENT_TYPE,
      ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
      CONTENT_TYPE -> MediaType.OCTET_STREAM.`type`)

  def index = Action(Ok(views.html.index()))

  def RedirectTo(url: String, withToken: Option[String]) = Redirect(url).withCookies(Cookie(
    name = AccessTokenKey,
    value = withToken.getOrElse(""),
    maxAge = withToken.map(_ => 14 * 3600 * 24).orElse(Some(-1)),
    domain = Some(ServerConfig.Domain),
    secure = false, //we don't have HTTPS yet
    httpOnly = true
  ))

  def loginWithGithub(code: String, source_url: String) = Action.async { request =>
    GithubApi.getGithubToken(code).flatMap { tokenOpt =>
      userCache.getOrFetchUserId(tokenOpt).map { _ =>
        RedirectTo(source_url, withToken = tokenOpt)
      }
    }
  }

  def joinEventWithGithub(joinEvent: Long, code: String, sourceUrl: String) = Action.async { request =>
    GithubApi.getGithubToken(code).flatMap { tokenOpt =>
      userCache.getOrFetchUserId(tokenOpt)
        .map { userOpt =>
          new ApiService(tokenOpt, userOpt, eventSource, userCache)
            .joinEventAndGetJoins(EventId(joinEvent))

          RedirectTo(sourceUrl, withToken = tokenOpt)
        }
    }
  }

  def flagEventWithGithub(flagEventById: Long, code: String, sourceUrl: String) = Action.async { request =>
    GithubApi.getGithubToken(code).flatMap { tokenOpt =>
      userCache.getOrFetchUserId(tokenOpt)
        .map { userOpt =>
          new ApiService(tokenOpt, userOpt, eventSource, userCache)
            .flagEvent(EventId(flagEventById))

          RedirectTo(sourceUrl, withToken = tokenOpt)
        }
    }
  }

  def autowireApi(path: String) = Action.async(parse.raw) { implicit request =>
    println(s"Request path: $path")

    val tokenCookie: Option[String] = request.cookies.get(Github.AccessTokenKey).map(_.value)

    val userFuture = userCache.getOrFetchUserId(tokenCookie)

    // get the request body as Array[Byte]
    val b = request.body.asBytes(parse.UNLIMITED).get
    val req = autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(ByteBuffer.wrap(b)))

    userFuture.flatMap { userOpt =>
      val apiService = new ApiService(tokenCookie, userOpt, eventSource, userCache)
      val router = Router.route[Api](apiService)

      // call Autowire route
      router(req).map(buffer => {
        val data = Array.ofDim[Byte](buffer.remaining())
        buffer.get(data)
        CorsEnabled(Ok(data))
      })
    }
  }

  /*Enables CORS*/
  def options(path: String) = Action { implicit request => CorsEnabled(NoContent) }
}
