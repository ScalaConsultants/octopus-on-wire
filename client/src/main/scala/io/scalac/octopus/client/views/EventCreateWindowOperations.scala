package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.DateOps
import org.scalajs.dom.Event
import org.scalajs.dom.html.Div
import org.scalajs.dom.KeyboardEvent

import scala.scalajs.js.{Date, timers}
import scala.util.{Failure, Try}
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
    val bottomArrow = div(`class` := "octopus-window-bottom-arrow arrow-center")
    val eventNameField = h1(
      placeholder := "Event name",
      `class` := "octopus-event-name", contenteditable := "true"
    ).render

    val startDateField = p(DateOps.dateToString(startDay), `class` := "octopus-event-date")

    val startHourField = span(placeholder := "H", contenteditable := "true", onkeypress := intOnlyKeyHandler _).render
    val startMinuteField = span(placeholder := "MM", contenteditable := "true", onkeypress := intOnlyKeyHandler _).render
    val endHourField = span(placeholder := "H", contenteditable := "true", onkeypress := intOnlyKeyHandler _).render
    val endMinuteField = span(placeholder := "MM", contenteditable := "true", onkeypress := intOnlyKeyHandler _).render

    val eventLocationField = p(placeholder := "Event location", contenteditable := "true").render

    val eventUrlField = p(
      placeholder := "Event url",
      `class` := "octopus-event-url",
      contenteditable := "true"
    ).render

    val window = div(
      `class` := "octopus-window octopus-event-creation closed",
      eventNameField,
      startDateField,
      p("From ", startHourField, ":", startMinuteField, " to ", endHourField, ":", endMinuteField),
      eventLocationField,
      eventUrlField,
      bottomArrow
    ).render

    openWindow(window, octopusHome, eventNameField.focus())
    Option(window)
  }

  def intOnlyKeyHandler(event: Event): Unit = event match {
    case e: KeyboardEvent =>
      Try(e.charCode.toChar.toString.toInt) match {
        case f: Failure[_] => event.preventDefault()
        case _ =>
      }
  }
}