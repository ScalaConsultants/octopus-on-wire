package io.scalac.octopus.client.views

import io.scalac.octopus.client.tools.EventDateOps
import org.scalajs.dom.html.Div

import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

/**
 * Manages the event detail window view.
 */
object EventWindowOperations extends WindowOperations {
  type EventWindowOption = Option[(Event, Div)]
  protected var eventWindow: EventWindowOption = None

  def openEventWindow(item: Event)(octopusHome: Div): Unit = {
    CalendarWindowOperations.closeWindow(octopusHome)
    eventWindow = switchEventWindow(item)(octopusHome)
  }

  protected def switchEventWindow(item: Event)(octopusHome: Div): EventWindowOption = eventWindow match {
    /*The event we want to display is the same as the one already displayed.
      Do nothing (return the same thing we matched)*/
    case Some((event, window)) if event.id == item.id =>
      closeWindow(octopusHome)
      None

    /*The window is visible, but the clicked event is another one.
      Close it and open a window for the clicked event*/
    case Some((_, window)) =>
      closeWindow(octopusHome)
      switchEventWindow(item)(octopusHome)

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

      openWindow(window)(octopusHome)
      Option((item, window))
  }

  override def closeWindow(octopusHome: Div): Unit = eventWindow = eventWindow match {
    case Some((_, openedWindow)) =>
      removeWindow(openedWindow)(octopusHome)
      None
    case None => None
  }
}

