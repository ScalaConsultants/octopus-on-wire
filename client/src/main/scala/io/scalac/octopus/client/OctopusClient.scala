package io.scalac.octopus.client

import autowire._
import org.scalajs.dom.html.{Div, UList}
import boopickle.Default._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetIntervalHandle
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

@JSExport("OctopusClient")
object OctopusClient extends js.JSApp {

  /**
   * Returns the next possible index.
   * If increasing the index would result in going over the bounds, returns 0.
   **/
  def nextIndex(i: Int, max: Int) = if (i == max - 1) 0 else i + 1

  /**
   * Called after updating the index.
   * Adds, removes appropriate classes to items in list
   **/
  def updateClasses(aList: UList, index: Int): Unit =
    for (i <- 0 until aList.childElementCount) {
      val elem = aList.children(i)

      i compareTo index match {

        // Item on the right
        case diff if diff > 0 =>
          elem.classList.add("right")
          elem.classList.remove("left")

        // Item on the left
        case diff if diff < 0 =>
          elem.classList.add("left")
          elem.classList.remove("right")

        // Current item - remove adjustment classes
        case _ =>
          elem.classList.remove("right")
          elem.classList.remove("left")
      }
    }

  @JSExport
  def buildWidget(root: Div): Unit = {
    println(s"Starting")

    var currentIndex = ClientConfig.InitialSlideIndex
    var intervalHandle: Option[SetIntervalHandle] = None

    var eventWindow: Option[(Event, Div)] = None

    lazy val list: UList = ul(
      onmouseover := {() => intervalHandle = stopInterval()},
      onmouseout := {() => intervalHandle = startInterval()}
    ).render

    lazy val octopusHome = div(id := "octopus-home", list).render

    lazy val outside: Div = div(`class` := "octopus-outside",
      onclick := {() => eventWindow = closeEventWindow()}
    ).render

    def openEventWindow(item: Event): Option[(Event, Div)] = eventWindow match{
      case Some((event, window)) if event.id == item.id =>
        eventWindow
      case Some((_, window))=>
          eventWindow = closeEventWindow()
          openEventWindow(item)
      case _ =>
        val window =
          div(`class` := "octopus-window closed",
            h1(item.name, `class` := "octopus-event-name"),
            p("date placeholder", `class` := "octopus-event-date"),
            p(item.location, `class` := "octopus-event-location"),
            div(`class` := "octopus-window-bottom-arrow arrow-center")
          ).render
        octopusHome.appendChild(window)
        octopusHome.appendChild(outside)
        timers.setTimeout(ClientConfig.WindowOpenDelay)(window.classList.remove("closed"))
        Some(item, window)
    }

    def closeEventWindow() = eventWindow match{
      case Some((_, window)) =>
        octopusHome.removeChild(outside)
        window.classList.add("closed")
        timers.setTimeout(ClientConfig.WindowLoadTime)(octopusHome.removeChild(window))
        None
      case None => None
    }

    def moveToNextItem() = {
      currentIndex = nextIndex(currentIndex, list.childElementCount)
      updateClasses(list, currentIndex)
    }

    def startInterval() = intervalHandle match {
      case Some(interval) => intervalHandle
      case None => Some(timers.setInterval(ClientConfig.ItemChangeInterval)(moveToNextItem()))
    }

    def stopInterval() = intervalHandle match {
      case Some(interval) =>
        timers.clearInterval(interval)
        None
      case None => None
    }

    AutowireClient[Api].getItems(ClientConfig.ItemsToFetch).call().foreach { items =>
      items foreach {
        item => list.appendChild(
          li(div(`class` := "item",
            span(item.name, onclick := { () => eventWindow = openEventWindow(item)}),
            div(`class` := "next", title := "Next", onclick := moveToNextItem _)
          )).render
        )
      }

      updateClasses(list, currentIndex)
      intervalHandle = startInterval()
    }

    root.appendChild(octopusHome)
  }


  @JSExport
  override def main(): Unit = ()
}
