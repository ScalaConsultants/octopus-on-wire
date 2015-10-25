package io.scalac.octopus.client

import org.scalajs.dom.html.Div

import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

object CalendarWindowOperations extends WindowOperations {
  type CalendarWindowOption = Option[Div]

  var calendarWindow: CalendarWindowOption = None

  def openCalendarWindow(events: Array[Event])(implicit octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    calendarWindow = getCalendarWindow(events)
  }

  def getCalendarWindow(events: Array[Event])(implicit octopusHome: Div): CalendarWindowOption = calendarWindow match {
    case Some(window) =>
      calendarWindow
    case None =>
      import DateOps._
      import EventDateOps._

      import scala.language.implicitConversions

      val window = div(
        CalendarTable(
          new Date(Date.now()),
          marker = calendarDay =>
            events.exists(_.days.exists(_ isSameDay calendarDay)),
          clickListener = { date =>
            /*todo display list of events on the clicked day to user*/
          }
        ),
        `class` := "octopus-window octopus-calendar closed",
        div(`class` := "octopus-window-bottom-arrow arrow-left")
      ).render
      openWindow(window)
      Option(window)
  }

  def closeWindow(implicit octopusHome: Div): Unit = calendarWindow = calendarWindow match {
    case Some(openedWindow) =>
      removeWindow(openedWindow)
      None
    case None => None
  }
}
