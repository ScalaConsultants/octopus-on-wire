package controllers

import java.nio.ByteBuffer

import boopickle.Default._
import com.google.common.net.MediaType
import config.Github._
import config.{Github, Router, ServerConfig}
import data._
import domain.{Events, UserIdentity}
import play.Play
import play.api.Logger
import play.api.mvc._
import services.{ApiService, GithubApi}
import tools.OffsetTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.EventId

object Application extends Controller {
  val eventSource: EventSource = new PersistentEventSource
  val userCache: UserCache = new PersistentUserCache

  //add dummy events on dev if no future events exist
  if (Play.isDev) {
    val futureEvents = Events.getFutureUnflaggedEvents(None, 1, OffsetTime.serverCurrent)
    futureEvents.filter(_.nonEmpty) foreach { _ => Logger.info("Future events exist, no adding") }
    def eventsFuture = Future.sequence(DummyData.events.map(Events.addEventAndGetId))
    def usersFuture = Future.sequence(DummyData.eventJoins.flatMap(_._2).map(userCache.getOrFetchUserInfo(_, None)))

    val pairs = DummyData.eventJoins.toList.flatMap {
      case (eventIndex, userIds) => userIds.map(uid => (eventIndex.value.toInt - 1, uid))
    }
    def joinsFuture(eventIds: Seq[EventId]) = Future.sequence(pairs.map {
      case (eventIndex, uid) => eventSource.joinEvent(uid, eventIds(eventIndex))
    })

    val addedJoins = for {
      futureEvents <- futureEvents
      if futureEvents.isEmpty
      events <- eventsFuture
      users <- usersFuture
      joins <- joinsFuture(events)
    } yield joins

    addedJoins foreach { _ => Logger.info("Added dummy events") }
  }

  def CorsEnabled(result: Result)(implicit request: Request[Any]): Result = {
    val newResult = result.withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers(ORIGIN),
      ACCESS_CONTROL_ALLOW_HEADERS -> CONTENT_TYPE,
      ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
      CONTENT_TYPE -> MediaType.OCTET_STREAM.`type`)

    request.headers.get(ORIGIN) match {
      case Some(origin) => newResult.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> origin)
      case _ => newResult
    }
  }

  def index = Action(Ok(views.html.index()))

  def plain = Action(Ok(views.html.plain()))

  def RedirectTo(url: String, withToken: Option[String]) = Redirect(url).withCookies(Cookie(
    name = AccessTokenKey,
    value = withToken.getOrElse(""),
    maxAge = withToken.map(_ => 14 * 3600 * 24).orElse(Some(-1)),
    domain = Some(ServerConfig.CookieDomain),
    secure = false, //we don't have HTTPS yet
    httpOnly = true
  ))

  def loginWithGithub(code: String, source_url: String) = Action.async { request =>
    GithubApi.getGithubToken(code).flatMap { tokenOpt =>
      userCache.getOrFetchUserId(tokenOpt).map { _ =>
        RedirectTo(source_url, withToken = Some(tokenOpt))
      }
    }
  }

  def joinEventWithGithub(joinEvent: Long, code: String, sourceUrl: String) = Action.async { request =>
    GithubApi.getGithubToken(code).flatMap { token =>
      userCache.getOrFetchUserId(token)
        .map { userId =>
          new ApiService(Some(UserIdentity(token, userId)), eventSource, userCache)
            .joinEventAndGetJoins(EventId(joinEvent))

          RedirectTo(sourceUrl, withToken = Some(token))
        }
    }
  }

  def flagEventWithGithub(flagEventById: Long, code: String, sourceUrl: String) = Action.async { request =>
    GithubApi.getGithubToken(code).flatMap { token =>
      userCache.getOrFetchUserId(token)
        .map { userId =>
          new ApiService(Some(UserIdentity(token, userId)), eventSource, userCache)
            .flagEvent(EventId(flagEventById))

          RedirectTo(sourceUrl, withToken = Some(token))
        }
    }
  }

  def autowireApi(path: String) = Action.async(parse.raw) { implicit request =>
    Logger.info(s"Request path: $path")

    val tokenCookie: Option[String] = request.cookies.get(Github.AccessTokenKey).map(_.value)

    val userIdFuture = tokenCookie.map(userCache.getOrFetchUserId)
      .getOrElse(Future.failed(new Exception("No token supplied")))

    // get the request body as Array[Byte]
    val b = request.body.asBytes(parse.UNLIMITED).get
    val req = autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(ByteBuffer.wrap(b)))

    userIdFuture
      .map { userId =>
        tokenCookie match {
          case Some(token) => Some(UserIdentity(token, userId))
          case _ => None
        }
      }
      .recover { case _ => None }
      .flatMap { userIdentityOpt =>
        val apiService = new ApiService(userIdentityOpt, eventSource, userCache)
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
