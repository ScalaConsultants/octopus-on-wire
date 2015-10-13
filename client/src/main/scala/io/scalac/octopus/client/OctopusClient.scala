package io.scalac.octopus.client

import autowire._
import boopickle.Default._
import org.scalajs.dom.html.Div
import scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import spatutorial.shared.Api
import org.scalajs.dom

@JSExport("OctopusClient")
object OctopusClient extends js.JSApp {

  @JSExport
  def buildWidget(root: Div): Unit = {
    println(s"Starting")

    AutowireClient[Api].hello().call().foreach { todos =>
      println(s"Got some things to do $todos")
      root.appendChild( span(todos).render )
    }
  }

  @JSExport
  override def main(): Unit = ()
}
