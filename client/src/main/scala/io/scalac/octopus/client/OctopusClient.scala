package io.scalac.octopus.client

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.config.ClientConfig._
import io.scalac.octopus.client.views.SliderViewOperations._
import io.scalac.octopus.client.views.{CalendarWindowOperations, EventWindowOperations}
import org.scalajs.dom.html.{Div, UList}

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport("OctopusClient")
object OctopusClient extends js.JSApp {

  val list: UList = ul(
    `class` := "octopus-shuffle-list",
    onmouseover := { () => slideIntervalHandle = stopSlideInterval() },
    onmouseout := { () => slideIntervalHandle = startSlideInterval(list) }
  ).render

  val octopusHome: Div = div(id := "octopus-home", list).render

  @JSExport
  def buildWidget(root: Div): Unit = {
    println(s"Starting")

    octoApi.getFutureItems(ClientConfig.ItemsToFetch).call().map { items =>
      items foreach {
        item => list.appendChild(
          li(div(`class` := "item",
            span(`class` := "calendar-icon", title := "All events", onclick := { () => CalendarWindowOperations.openCalendarWindow(octopusHome) }),
            span(`class` := "item-name", item.name, onclick := { () => EventWindowOperations.openEventWindow(item.id, octopusHome) }),
            div(`class` := "next", title := "Next", onclick := { () => moveToNextItem(list) })
          )).render
        )
      }

      updateClasses(list)
      slideIntervalHandle = startSlideInterval(list)
    }

    root.appendChild(octopusHome)
  }

  @JSExport
  override def main(): Unit = ()
}