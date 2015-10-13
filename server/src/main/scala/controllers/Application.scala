package controllers

import java.nio.ByteBuffer

import boopickle.Default._
import play.api.mvc._
import services.ApiService
import spatutorial.shared.Api

import scala.concurrent.ExecutionContext.Implicits.global

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

  def autowireApi(path: String) = Action.async(parse.raw) { implicit request =>
    println(s"Request path: $path")

    // get the request body as Array[Byte]
    val b = request.body.asBytes(parse.UNLIMITED).get
    val req = autowire.Core.Request(path.split("/"), Unpickle[Map[String, ByteBuffer]].fromBytes(ByteBuffer.wrap(b)))

    // call Autowire route
    router(req).map(buffer => {
      val data = Array.ofDim[Byte](buffer.remaining())
      buffer.get(data)
      Ok(data)
    })
  }
}
