package io.scalac.octopus.client

import java.nio.ByteBuffer

import boopickle.Default._
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.typedarray._

object AutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {

  override def doCall(req: Request): Future[ByteBuffer] = {
    println(s"AutowireClient calling $req")
    dom.ext.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      headers = Map("Content-Type" -> "application/octet-stream"),
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer"
    ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
  }

  override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)

  override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
}
