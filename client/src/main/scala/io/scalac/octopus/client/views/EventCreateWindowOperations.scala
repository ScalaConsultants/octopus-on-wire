package io.scalac.octopus.client.views

import io.scalac.octopus.client.tools.DateOps
import org.scalajs.dom.html.Div

import scala.scalajs.js.Date
import scalatags.JsDom.all._

object EventCreateWindowOperations extends WindowOperations {

  type CreationWindow = Option[Div]
  private var creationWindow: CreationWindow = None

  override def closeWindow(octopusHome: Div): Unit = creationWindow = creationWindow match {
    case Some(window) =>
      removeWindow(window, octopusHome)
      None
    case None => None
  }

  def closeOthersAndOpenWindow(eventStart: Date, eventEnd: Date, octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    CalendarWindowOperations.closeWindow(octopusHome)
    openCreationWindow(eventStart, eventEnd, octopusHome)
  }

  def openCreationWindow(startDay: Date, endDat: Date, octopusHome: Div) = creationWindow = {
    val bottomArrow = div(`class` := "octopus-window-bottom-arrow arrow-center")
    val eventName = h1(
      placeholder := "Event name",
      `class` := "octopus-event-name", contenteditable := "true"
    ).render

    val startDate = p(DateOps.dateToString(startDay), `class` := "octopus-event-date")
    val endDate = p(DateOps.dateToString(endDat), `class` := "octopus-event-date")

    val window = div(
      `class` := "octopus-window octopus-event-creation closed",
      eventName,
      startDate,
      endDate,
      p(
        placeholder := "Event location",
        `class` := "octopus-event-location",
        contenteditable := "true"
      ),

      /* other fields */

      bottomArrow
    ).render

    openWindow(window, octopusHome, eventName.focus())
    Option(window)
  }

}