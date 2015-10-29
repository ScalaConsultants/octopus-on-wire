package controllers

import java.nio.ByteBuffer

import boopickle.Default._
import com.google.common.net.MediaType
import play.api.mvc._
import services.ApiService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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

  val router = Router.route[Api](apiService)

  def CorsEnabled(result: Result)(implicit request: Request[Any]): Result =
    result.withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> request.headers(ORIGIN),
      ACCESS_CONTROL_ALLOW_HEADERS -> CONTENT_TYPE,
      CONTENT_TYPE -> MediaType.OCTET_STREAM.`type`
    )

  def autowireApi(path: String) = Action.async(parse.raw) { implicit request =>
    println(s"Request path: $path")

    // get the request body as Array[Byte]
    val b = request.body.asBytes(parse.UNLIMITED).get
    val req = autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(ByteBuffer.wrap(b)))

    // call Autowire route
    router(req).map(buffer => {
      val data = Array.ofDim[Byte](buffer.remaining())
      buffer.get(data)
      CorsEnabled(
        Ok(data)
      )
    })
  }



  /*Enables CORS*/
  def options(path: String) = Action(implicit request => CorsEnabled(NoContent))
}
