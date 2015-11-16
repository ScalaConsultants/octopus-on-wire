package io.scalac.octopus.client.views

import org.scalajs.dom.html.Div

import scala.scalajs.js.Date
import scalatags.JsDom.all._

object CalendarWindowOperations extends WindowOperations {
  type CalendarWindowOption = Option[Div]

  protected var calendarWindow: CalendarWindowOption = None
  private var isUserSelectingDate = false

  def openCalendarWindow(octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    EventCreateWindowOperations.closeWindow(octopusHome)
    calendarWindow = switchCalendarWindow(octopusHome)
  }

  def switchCalendarWindow(octopusHome: Div): CalendarWindowOption =
    calendarWindow match {
      case Some(window) =>
        closeWindow(octopusHome)
        None
      case None =>
        val now = new Date(Date.now())

        val window: Div = div(
          div(),
          `class` := "octopus-window octopus-calendar closed",
          div(),
          div(`class` := "octopus-window-bottom-arrow arrow-left")
        ).render

        val calendarView = new EventCalendar(window, octopusHome)

        window.replaceChild(calendarView(now), window.firstChild)

        val addEventButton: Div = div(
          `class` := "octopus-calendar-create-event",
          "Add your own ", i(`class` := "fa fa-plus"),
          onclick := { () =>
            isUserSelectingDate = true
            window.replaceChild(new DateSelector(window, octopusHome).apply(now), window.firstChild)
            window.replaceChild(span(`class` := "select-date-prompt", "When is the event?").render, window.childNodes(1))
          }
        ).render

        window.replaceChild(addEventButton, window.childNodes(1))
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