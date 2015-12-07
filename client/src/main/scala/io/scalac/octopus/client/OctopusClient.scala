package io.scalac.octopus.client

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.config.ClientConfig._
import io.scalac.octopus.client.views.SliderViewOperations._
import io.scalac.octopus.client.views.calendar.EventCalendarWindow
import io.scalac.octopus.client.views.SliderViewOperations
import io.scalac.octopus.client.views.detail.EventDetailWindow
import org.scalajs.dom.html.{Div, UList}
import org.scalajs.dom.raw.MouseEvent

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all.{list => _, _}

@JSExport("OctopusClient")
object OctopusClient extends js.JSApp {
  val octopusHome: Div = div(id := "octopus-home", list).render

  val calendarIcon = span(
    `class` := "calendar-icon",
    title := "All events",
    onclick := { () => EventCalendarWindow.open(octopusHome, new Date(Date.now)) }
  )

  val itemPlaceholder = li(
    div(
      `class` := "item",
      calendarIcon,
      span(`class` := "item-name placeholder", ClientConfig.EmptyListPlaceholderText)
    )
  ).render

  @JSExport
  def buildWidget(root: Div): Unit = {
    println(s"Starting")

    val fakeEventAdder = button(
      "Add fake events if needed (requires being logged in)",
      `class` := "add-fake-events-button",
      onclick := {
        (e: MouseEvent) => octoApi.addFakeEvents().call().foreach { _ =>
          root.removeChild(e.srcElement)
        }
      }
    ).render

    root.appendChild(fakeEventAdder)

    root.appendChild(octopusHome)
    refreshEvents(list, octopusHome)
  }

  def refreshEvents(list: UList, octopusHome: Div): Unit = {

    octoApi.getFutureItems(ClientConfig.ItemsToFetch).call().map { items =>

      while (list.hasChildNodes) list.removeChild(list.firstChild)
      stopSlideInterval()

      items foreach {
        item => list.appendChild(
          li(div(`class` := "item",

            //calendar icon
            span(`class` := "calendar-icon", title := "All events",
              onclick := { () => EventCalendarWindow.open(octopusHome, new Date(Date.now())) }),

            //event preview
            span(`class` := "item-name", item.name,
              onclick := { () => EventDetailWindow.open(item.id, octopusHome) }),

            //next icon. Don't show it if there is nothing to slide to
            if (items.length > 1)
              div(`class` := "next", title := "Next", onclick := { () => SliderViewOperations.moveToNextItem(list) })
            else ""

          )).render
        )
      }

      if (items.isEmpty) list.appendChild(itemPlaceholder)
      else {
        SliderViewOperations.currentIndex = ClientConfig.InitialSlideIndex
        updateClasses(list)
        startSlideInterval(list)
      }
    }
  }

  @JSExport
  override def main(): Unit = ()
}