package io.scalac.octopus.client

import autowire._
import org.scalajs.dom.html.{Div, UList}
import spatutorial.shared.Api
import boopickle.Default._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetIntervalHandle
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

    lazy val list: UList = ul(
      onmouseover := {() => intervalHandle = stopInterval()},
      onmouseout := {() => intervalHandle = startInterval()}
    ).render

    def moveToNextItem() = {
      currentIndex = nextIndex(currentIndex, list.childElementCount)
      updateClasses(list, currentIndex)
    }

    def startInterval() = intervalHandle match {
      case Some(interval) => Some(interval)
      case None => Some(timers.setInterval(ClientConfig.ItemChangeInterval)(moveToNextItem()))
    }

    def stopInterval() = intervalHandle match{
      case Some(interval) =>
        timers.clearInterval(interval)
        None
      case None => None
    }

    AutowireClient[Api].getItems(ClientConfig.ItemsToFetch).call().foreach { items =>
      items foreach {
        item => list.appendChild(
          li(div(`class` := "item", span(item.name), div(`class` := "next", title := "Next", onclick := moveToNextItem _))).render
        )
      }

      updateClasses(list, currentIndex)
      intervalHandle = startInterval()
    }

    root.appendChild(list.render)
  }


  @JSExport
  override def main(): Unit = ()
}
