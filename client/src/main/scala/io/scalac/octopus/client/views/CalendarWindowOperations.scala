package io.scalac.octopus.client.views

import org.scalajs.dom.html.Div

import scala.scalajs.js.Date
import scalatags.JsDom.all._

object CalendarWindowOperations extends WindowOperations {
  type CalendarWindowOption = Option[Div]

  protected var calendarWindow: CalendarWindowOption = None

  def openCalendarWindow(octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    calendarWindow = switchCalendarWindow(octopusHome)
  }

  def openEventCreationWindow() = ???

  def switchCalendarWindow(octopusHome: Div): CalendarWindowOption =
    calendarWindow match {
      case Some(window) =>
        closeWindow(octopusHome)
        None
      case None =>

        val now = new Date(Date.now())

        val addEventButton: Div = div(
          `class` := "octopus-calendar-create-event",
          "Add your own ", i(`class` := "fa fa-plus"),
          onclick := { () => openEventCreationWindow() }
        ).render

        val window: Div = div(
          div(),
          `class` := "octopus-window octopus-calendar closed",
          addEventButton,
          div(`class` := "octopus-window-bottom-arrow arrow-left")
        ).render

        val calendarView = new CalendarView(window, octopusHome)

        window.replaceChild(calendarView(now), window.firstChild)
        openWindow(window, octopusHome)
        Option(window)
    }

  def closeWindow(octopusHome: Div): Unit = calendarWindow = calendarWindow match {
    case Some(openedWindow) =>
      removeWindow(openedWindow, octopusHome)
      None
    case None => None
  }
}