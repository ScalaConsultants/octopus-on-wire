package io.scalac.octopus.client

import autowire._
import io.scalac.octopus.client.views.{SliderViewOperations, EventWindowOperations, CalendarWindowOperations}
import SliderViewOperations._
import io.scalac.octopus.client.config.{ClientConfig, AutowireClient}
import io.scalac.octopus.client.views.{EventWindowOperations, CalendarWindowOperations}
import org.scalajs.dom.html.{Div, UList}

import boopickle.Default._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalac.octopusonwire.shared.Api
import scalatags.JsDom.all._

@JSExport("OctopusClient")
object OctopusClient extends js.JSApp {

  val list: UList = ul(
    onmouseover := { () => slideIntervalHandle = stopSlideInterval() },
    onmouseout := { () => slideIntervalHandle = startSlideInterval(list) }
  ).render

  implicit val octopusHome: Div = div(id := "octopus-home", list).render

  @JSExport
  def buildWidget(root: Div): Unit = {
    println(s"Starting")

    AutowireClient[Api].getItems(ClientConfig.ItemsToFetch).call().map { items =>
      items foreach {
        item => list.appendChild(
          li(div(`class` := "item",
            span(`class` := "calendar-icon", title := "All events", onclick := { () => CalendarWindowOperations.openCalendarWindow(items) }),
            span(item.name, onclick := { () => EventWindowOperations.openEventWindow(item) }),
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