package io.scalac.octopus.client.views

import org.scalajs.dom.html.Div

import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

object CalendarWindowOperations extends WindowOperations {
  type CalendarWindowOption = Option[Div]

  protected var calendarWindow: CalendarWindowOption = None

  /*todo replace events parameter with an API call to events in given month*/
  def openCalendarWindow(events: Array[Event])(octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    calendarWindow = switchCalendarWindow(events)(octopusHome)
  }

  def switchCalendarWindow(events: Array[Event])(octopusHome: Div): CalendarWindowOption =
    calendarWindow match {
      case Some(window) =>
        closeWindow(octopusHome)
        None
      case None =>

        val now = new Date(Date.now())

        val window: Div = div(
          div(),
          `class` := "octopus-window octopus-calendar closed",
          div(`class` := "octopus-window-bottom-arrow arrow-left")
        ).render

        val calendarView = new CalendarView(window, octopusHome)

        window.replaceChild(calendarView(now, events), window.firstChild)
        openWindow(window)(octopusHome)
        Option(window)
    }

  def closeWindow(octopusHome: Div): Unit = calendarWindow = calendarWindow match {
    case Some(openedWindow) =>
      removeWindow(openedWindow)(octopusHome)
      None
    case None => None
  }
}