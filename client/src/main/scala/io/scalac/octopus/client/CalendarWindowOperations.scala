package io.scalac.octopus.client

import org.scalajs.dom.html.Div
import  scalatags.JsDom.all._

object CalendarWindowOperations extends WindowOperations{
  var calendarWindow: Option[Div] = None


  def openCalendarWindow(implicit octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    calendarWindow = getCalendarWindow
  }

  def getCalendarWindow(implicit octopusHome: Div): Option[Div] = calendarWindow match{
    case Some(window) =>
      calendarWindow
    case None =>
      val window = div("hello world", `class` := "octopus-window closed").render
      openWindow(window)
      Some(window)
  }

  def closeWindow(implicit octopusHome: Div): Unit = calendarWindow = calendarWindow match {
    case Some(openedWindow) =>
      removeWindow(openedWindow)
      None
    case None => None
  }
}
