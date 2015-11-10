package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import org.scalajs.dom.html.UList

import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetIntervalHandle
import scalatags.JsDom.all._

object SliderViewOperations {
  var slideIntervalHandle: Option[SetIntervalHandle] = None

  var currentIndex = ClientConfig.InitialSlideIndex

  val list: UList = ul(
    `class` := "octopus-shuffle-list",
    onmouseover := { () => stopSlideInterval() },
    onmouseout := { () => startSlideInterval(list) }
  ).render

  /**
   * Returns the next possible index.
   * If increasing the index would result in going over the bounds, returns 0.
   **/
  def nextIndex(i: Int, max: Int) = if (i == max - 1) 0 else i + 1

  /**
   * Called after updating the index.
   * Adds, removes appropriate classes to items in list
   **/
  def updateClasses(list: UList): Unit =
    for (i <- 0 until list.childElementCount) {
      val elem = list.children(i)

      i compareTo currentIndex match {

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

  def moveToNextItem(list: UList) = {
    currentIndex = nextIndex(currentIndex, list.childElementCount)
    updateClasses(list)
  }

  def startSlideInterval(list: UList) = slideIntervalHandle = slideIntervalHandle match {
    case Some(interval) => slideIntervalHandle
    case None => Some(timers.setInterval(ClientConfig.ItemChangeInterval)(moveToNextItem(list)))
  }

  def stopSlideInterval() = slideIntervalHandle = slideIntervalHandle match {
    case Some(interval) =>
      timers.clearInterval(interval)
      None
    case None => None
  }

}
