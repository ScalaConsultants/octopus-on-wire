package io.scalac.octopus.client

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.config.ClientConfig._
import io.scalac.octopus.client.views.SliderViewOperations.{list, startSlideInterval, updateClasses}
import io.scalac.octopus.client.views.{CalendarWindowOperations, EventWindowOperations, SliderViewOperations}
import org.scalajs.dom.html.Div

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport("OctopusClient")
object OctopusClient extends js.JSApp {

  @JSExport
  def buildWidget(root: Div): Unit = {
    println(s"Starting")

    val octopusHome: Div = div(id := "octopus-home").render
    root.appendChild(octopusHome)

    octoApi.getFutureItems(ClientConfig.ItemsToFetch).call().map { items =>
      octopusHome.appendChild(list)

      items foreach {
        item => list.appendChild(
          li(div(`class` := "item",
            span(`class` := "calendar-icon", title := "All events", onclick := { () => CalendarWindowOperations.openCalendarWindow(octopusHome) }),
            span(`class` := "item-name", item.name, onclick := { () => EventWindowOperations.openEventWindow(item.id, octopusHome) }),
            div(`class` := "next", title := "Next", onclick := { () => SliderViewOperations.moveToNextItem(list) })
          )).render
        )
      }

      updateClasses(list)
      startSlideInterval(list)
    }
  }

  @JSExport
  override def main(): Unit = ()
}