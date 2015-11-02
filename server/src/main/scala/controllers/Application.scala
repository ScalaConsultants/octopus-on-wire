package controllers

import java.nio.ByteBuffer

import boopickle.Default._
import com.google.common.net.MediaType
import config.Github._
import play.api.libs.ws.WS
import play.api.mvc._
import services.ApiService
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
  val apiService = new ApiService()

  def index = Action {
    Ok(views.html.index())
  }

  def githubAuthorize(code: String, sourceUrl: String) = Action { request =>
    val result = Await.result(
      awaitable = WS url AccessTokenUrl
        withRequestTimeout 5000
        withHeaders ACCEPT -> "application/xml"
        withQueryString("client_id" -> ClientId, "client_secret" -> ClientSecret, "code" -> code)
        execute "POST", atMost = Duration.Inf)

    val token = (result.xml \ AccessToken).text

    Redirect(sourceUrl).withCookies(Cookie(
      name = AccessToken,
      value = token,
      maxAge = Option(14 * 3600 * 24),
      domain = None, //if not on localhost: Some(".octowire.com"),
      secure = false, //we don't have HTTPS yet
      httpOnly = true
    ))
  }

  val router = Router.route[Api](apiService)

  def CorsEnabled(result: Result)(implicit request: Request[Any]): Result = result.withHeaders(
    ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers(ORIGIN),
    ACCESS_CONTROL_ALLOW_HEADERS -> CONTENT_TYPE,
    CONTENT_TYPE -> MediaType.OCTET_STREAM.`type`)

  def autowireApi(path: String) = Action.async(parse.raw) { implicit request =>
    println(s"Request path: $path")

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
