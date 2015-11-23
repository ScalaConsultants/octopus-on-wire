package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import org.scalajs.dom.html.Div

import scala.scalajs.js.{Date, timers}
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
    timers.setTimeout(ClientConfig.WindowOpenDelay)(openCreationWindow(eventStart, octopusHome))
  }

  def openCreationWindow(startDay: Date, octopusHome: Div) = creationWindow = {
    val formHandler = new FormHandler(startDay, octopusHome)
    val window =
      div(
        `class` := "octopus-window octopus-event-creation closed",
        formHandler.view,
        div(`class` := "octopus-window-bottom-arrow arrow-left")
      ).render

    openWindow(window, octopusHome, formHandler.eventNameField.focus())
    Option(window)
  }
}