package controllers

import java.nio.ByteBuffer

import boopickle.Default._
import com.google.common.net.HttpHeaders._
import com.google.common.net.MediaType
import com.google.inject.Inject
import config.Github._
import config.{Github, Router, ServerConfig}
import data._
import domain.EventDao
import play.api.mvc._
import play.api.{Environment, Logger, Mode}
import services.{ApiService, GithubApi}
import tools.OffsetTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.EventId

class Application @Inject()(eventSource: PersistentEventSource, userCache: PersistentUserCache,
                            githubApi: GithubApi, env: Environment, eventDao: EventDao) extends Controller {

  //add dummy events on dev if no future events exist
  if (env.mode == Mode.Dev) {
    val futureEvents = eventDao.getFutureUnflaggedEvents(None, 1, OffsetTime.serverCurrent)
    futureEvents.filter(_.nonEmpty) foreach { _ => Logger.info("Future events exist, no adding") }
    def eventsFuture = Future.sequence(DummyData.events.map(eventDao.addEventAndGetId))
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

  def index = Action(Ok(views.html.index()))

  def plain = Action(Ok(views.html.plain()))

  def loginWithGithub(code: String, source_url: String) = Action.async { request =>
    githubApi.getGithubToken(code).flatMap { tokenOpt =>
      userCache.getOrFetchUserId(tokenOpt).map { _ =>
        RedirectWithToken(source_url, withToken = tokenOpt)
      }
    }
  }

  def joinEventWithGithub(joinEvent: Long, code: String, sourceUrl: String) = Action.async { request =>
    githubApi.getGithubToken(code).flatMap { tokenOpt =>
      userCache.getOrFetchUserId(tokenOpt)
        .map { userOpt =>
          new ApiService(tokenOpt, userOpt, eventSource, userCache)
            .joinEventAndGetJoins(EventId(joinEvent))

          RedirectWithToken(sourceUrl, withToken = tokenOpt)
        }
    }
  }

  def flagEventWithGithub(flagEventById: Long, code: String, sourceUrl: String) = Action.async { request =>
    githubApi.getGithubToken(code).flatMap { tokenOpt =>
      userCache.getOrFetchUserId(tokenOpt)
        .map { userOpt =>
          new ApiService(tokenOpt, userOpt, eventSource, userCache)
            .flagEvent(EventId(flagEventById))

          RedirectWithToken(sourceUrl, withToken = tokenOpt)
        }
    }
  }

  def autowireApi(path: String) = CorsEnabled {
    Action.async(parse.raw) { implicit request =>
      println(s"Request path: $path")

      val tokenCookie: Option[String] = request.cookies.get(Github.AccessTokenKey).map(_.value)

      val userFuture = userCache.getOrFetchUserId(tokenCookie)

      // get the request body as Array[Byte]
      val b = request.body.asBytes(parse.UNLIMITED).get
      val req = autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(b.asByteBuffer))

      userFuture.flatMap { userOpt =>
        val apiService = new ApiService(tokenCookie, userOpt, eventSource, userCache)
        val router = Router.route[Api](apiService)

        // call Autowire route
        router(req).map(buffer => {
          val data = Array.ofDim[Byte](buffer.remaining())
          buffer.get(data)
          Ok(data)
        })
      }
    }
  }

  /*Enables CORS*/
  def options(path: String) = CorsEnabled(Action(NoContent))
}

case class CorsEnabled[A](action: Action[A]) extends Action[A] {
  lazy val parser = action.parser

  override def apply(request: Request[A]): Future[Result] =
    action(request).map { result =>
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
}

object RedirectWithToken extends Results {
  def apply(url: String, withToken: Option[String]): Result = Redirect(url).withCookies(Cookie(
    name = AccessTokenKey,
    value = withToken.getOrElse(""),
    maxAge = withToken.map(_ => 14 * 3600 * 24).orElse(Some(-1)),
    domain = Some(ServerConfig.Domain),
    secure = false, //we don't have HTTPS yet
    httpOnly = true
  ))
}