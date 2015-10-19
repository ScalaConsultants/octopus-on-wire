package io.scalac.octopus.client

import autowire._
import org.scalajs.dom.html._
import spatutorial.shared.Api

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport("OctopusClient")
object OctopusClient extends js.JSApp {

  @JSExport
  def buildWidget(root: Div): Unit = {
    println(s"Starting")

    val list: UList = ul.render
    AutowireClient[Api].getItems(3).call().foreach {
      _.foreach {
        item => list.appendChild(li(item.name).render)
      }
    }
    root.appendChild(list.render)
  }

  @JSExport
  override def main(): Unit = ()
}
