package io.scalac.octopus.client

import org.scalajs.dom.html.Div

import scala.scalajs.js.timers
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

/**
 * Manages the event detail window view.
 */
object EventWindowOperations extends WindowOperations {
  protected var eventWindow: Option[(Event, Div)] = None

  def openEventWindow(item: Event)(implicit octopusHome: Div): Unit = {
    CalendarWindowOperations.closeWindow(octopusHome)
    eventWindow = getEventWindow(item)
  }

  protected def getEventWindow(item: Event)(implicit octopusHome: Div): Option[(Event, Div)] = eventWindow match {
    /*The event we want to display is the same as the one already displayed.
      Do nothing (return the same thing we matched)*/
    case Some((event, window)) if event.id == item.id =>
      eventWindow

    /*The window is visible, but the clicked event is another one.
      Close it and open a window for the clicked event*/
    case Some((_, window)) =>
      closeWindow(octopusHome)
      getEventWindow(item)

    /*The window is not opened. Open it.*/
    case _ =>
      import EventDateOps._
      val window =
      div(`class` := "octopus-window closed",
          h1(item.name, `class` := "octopus-event-name"),
          p(item.datesToString, `class` := "octopus-event-date"),
          p(item.location, `class` := "octopus-event-location"),
          a(href := item.url, `class` := "octopus-event-link", target := "_blank"),
          div(`class` := "octopus-window-bottom-arrow arrow-center")
        ).render

      openWindow(window)
      Some(item, window)
  }

  override def closeWindow(implicit octopusHome: Div): Unit = eventWindow = eventWindow match {
    case Some((_, openedWindow)) =>
      super.removeWindow(openedWindow)
      None
    case None => None
  }
}

