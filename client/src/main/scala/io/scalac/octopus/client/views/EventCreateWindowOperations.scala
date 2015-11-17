package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.config.ClientConfig.octoApi
import io.scalac.octopus.client.tools.DateOps
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.MouseEvent
import org.scalajs.dom.{Event, KeyboardEvent}

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.{Date, timers}
import scala.util.{Failure, Try}
import scalac.octopusonwire.shared.domain
import scalac.octopusonwire.shared.domain.{Success, EventId, Invalid}
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

  def intOnlyKeyHandler(maxLength: Int)(event: Event): Unit = event match {
    case e: KeyboardEvent =>
      val oldContent = e.srcElement.textContent
      Try(e.charCode.toChar.toString.toInt) match {
        case f: Failure[_] =>
          event.preventDefault()
        case _ => timers.setTimeout(50) {
          if (e.srcElement.textContent.length > maxLength)
            e.srcElement.textContent = oldContent
        }
      }
  }

  def openCreationWindow(startDay: Date, octopusHome: Div) = creationWindow = {
    val bottomArrow = div(`class` := "octopus-window-bottom-arrow arrow-center")
    val eventNameField = h1(
      placeholder := "Event name",
      `class` := "octopus-event-name", contenteditable := "true"
    ).render

    val startDateField = p(DateOps.dateToString(startDay), `class` := "octopus-event-date")

    val startHourField = getNewTimeField("H")
    val startMinuteField = getNewTimeField("MM")
    val endHourField = getNewTimeField("H")
    val endMinuteField = getNewTimeField("MM")

    val eventLocationField = p(placeholder := "Event location", contenteditable := "true").render

    val eventUrlField = p(
      placeholder := "Event url",
      contenteditable := "true",
      `class` := "octopus-event-url"
    ).render

    val messageField = p(`class` := "octopus-message hidden").render

    def isFormValid = Try(startHourField.textContent.toInt < 24 &&
      startMinuteField.textContent.toInt < 60 &&
      endHourField.textContent.toInt < 24 &&
      endMinuteField.textContent.toInt < 60 &&
      eventNameField.textContent.length >= 1 &&
      eventLocationField.textContent.length >= 1 &&
      eventUrlField.textContent.length >= 1).getOrElse(false)

    val submitButton: Div = div("Submit", `class` := "octopus-event-submit-button").render

    def sendEvent() = {
      messageField.classList.remove("hidden")
      messageField.textContent = "Submitting event, please wait..."
      submitButton.classList.add("hidden")

      /*TODO fix time offsets*/
      val startMillis = (startDay
        + (startHourField.textContent.toInt hours)
        + (startMinuteField.textContent.toInt minutes)).valueOf().toLong
      val endMillis = (startDay
        + (endHourField.textContent.toInt hours)
        + (endMinuteField.textContent.toInt minutes)).valueOf().toLong

      val event = new domain.Event(
        id = EventId(-1),
        name = eventNameField.textContent,
        startDate = startMillis,
        endDate = endMillis,
        location = eventLocationField.textContent,
        url = eventUrlField.textContent
      )

      octoApi.addEvent(event).call().foreach {
        case Success() =>
          messageField.classList.remove("hidden")
          messageField.textContent = s"Successfully added event."
          timers.setTimeout(2000)(closeWindow(octopusHome))

        case Invalid(arg) =>
          messageField.classList.remove("hidden")
          messageField.textContent = s"Invalid $arg"
          submitButton.classList.remove("hidden")

        case _ =>
          println("Unknown message received")
          submitButton.classList.remove("hidden")
      }
    }

    def buttonListener(me: MouseEvent) =
      if (isFormValid) sendEvent()
      else {
        messageField.textContent = "Please fill in all required fields properly."
        messageField.classList.remove("hidden")
      }

    submitButton.onclick = buttonListener _

    val window = div(
      `class` := "octopus-window octopus-event-creation closed",
      eventNameField,
      startDateField,
      p("From ", startHourField, ":", startMinuteField, " to ", endHourField, ":", endMinuteField),
      eventLocationField,
      eventUrlField,
      messageField,
      submitButton,
      bottomArrow
    ).render

    openWindow(window, octopusHome, eventNameField.focus())
    Option(window)
  }

  def getNewTimeField(hint: String) = span(placeholder := hint, contenteditable := "true", onkeypress := intOnlyKeyHandler(2) _).render
}