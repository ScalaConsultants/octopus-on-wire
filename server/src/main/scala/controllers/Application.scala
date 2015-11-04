package controllers

import java.nio.ByteBuffer

import boopickle.Default._
import com.google.common.net.MediaType
import config.Github
import config.Github._
import play.api.libs.ws.WS
import play.api.mvc._
import services.{EventSource, ApiService}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.language.postfixOps
import play.api.Play.current
import scalac.octopusonwire.shared.Api

object Router extends autowire.Server[ByteBuffer, Pickler, Pickler] {
  override def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)

  override def write[R: Pickler](r: R) = Pickle.intoBytes(r)
}

object Application extends Controller {
  def index = Action {
    Ok(views.html.index())
  }

  def getGithubToken(code: String): String = {
    val result = Await.result(
      awaitable = WS url AccessTokenUrl
        withRequestTimeout 5000
        withHeaders ACCEPT -> "application/xml"
        withQueryString("client_id" -> ClientId, "client_secret" -> ClientSecret, "code" -> code)
        execute "POST", atMost = Duration.Inf)

    (result.xml \ AccessToken).text
  }

  def joinEventWithGithub(joinEvent: Long, code: String, sourceUrl: String) = Action { request =>
    val token = getGithubToken(code)

    EventSource.joinEvent(Option(token), joinEvent)

    Redirect(sourceUrl).withCookies(Cookie(
      name = AccessToken,
      value = token,
      maxAge = Option(14 * 3600 * 24),
      domain = Some(".octowire.com"),
      secure = false, //we don't have HTTPS yet
      httpOnly = true
    ))
  }


  def CorsEnabled(result: Result)(implicit request: Request[Any]): Result = result.withHeaders(
    ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers(ORIGIN),
    ACCESS_CONTROL_ALLOW_HEADERS -> CONTENT_TYPE,
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
    CONTENT_TYPE -> MediaType.OCTET_STREAM.`type`)

  def autowireApi(path: String) = Action.async(parse.raw) { implicit request =>
    println(s"Request path: $path")

    val tokenCookie: Option[String] = request.cookies.get(Github.AccessToken).map(_.value)

    val apiService = new ApiService(tokenCookie)

    val router = Router.route[Api](apiService)

    // get the request body as Array[Byte]
    val b = request.body.asBytes(parse.UNLIMITED).get
    val req = autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(ByteBuffer.wrap(b)))

    // call Autowire route
    router(req).map(buffer => {
      val data = Array.ofDim[Byte](buffer.remaining())
      buffer.get(data)
      CorsEnabled(Ok(data))
    })
  }

  /*Enables CORS*/
  def options(path: String) = Action { implicit request => CorsEnabled(NoContent) }
}
