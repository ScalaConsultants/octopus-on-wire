package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig.{KeyCheckDelay, MoveToCalendarDelay, WindowOpenDelay, octoApi}
import io.scalac.octopus.client.tools.{FindSuffixInMessage, SuffixFound, DateOps}
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.views.FormHandler.getNewTimeField
import org.scalajs.dom.html.{Button, Div}
import org.scalajs.dom.raw.{HTMLElement, MouseEvent}
import org.scalajs.dom.{Event => DomEvent, KeyboardEvent}

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.{Date, timers}
import scala.util.{Failure, Success, Try}
import scalac.octopusonwire.shared.domain.Event.{InvalidDatesMessage, InvalidLocationMessage, InvalidNameMessage, InvalidURLMessage}
import scalac.octopusonwire.shared.domain.{Event, EventId, Invalid, Success => SuccessMessage}
import scalac.octopusonwire.shared.tools.IntRangeOps.int2IntRangeOps
import scalatags.JsDom.all._

class FormHandler(startDay: Date, octopusHome: Div) {
  val eventNameField = h1(
    placeholder := "Event name",
    `class` := "octopus-event-name", contenteditable := "true"
  ).render

  val startDateField = p(DateOps.dateToString(startDay), `class` := "octopus-event-date").render

  val startHourField = getNewTimeField("H", 0, 23)
  val startMinuteField = getNewTimeField("MM", 0, 59)
  val endHourField = getNewTimeField("H", 0, 23)
  val endMinuteField = getNewTimeField("MM", 0, 59)

  val dateFieldWrapper = p(
    "From ", startHourField, ":", startMinuteField, " to ", endHourField, ":", endMinuteField
  ).render

  val eventLocationField = p(placeholder := "Event location", contenteditable := "true").render

  val eventUrlField = p(
    placeholder := "Event url",
    contenteditable := "true",
    `class` := "octopus-event-url"
  ).render

  val messageField = p(`class` := "octopus-message hidden").render

  val submitButton: Button = button("Submit", `class` := "octopus-event-submit-button").render

  submitButton.onclick = submit _

  def view: List[HTMLElement] = List(
    eventNameField,
    startDateField,
    dateFieldWrapper,
    eventLocationField,
    eventUrlField,
    messageField,
    submitButton
  )

  def getMillisFrom(hourField: HTMLElement, minuteField: HTMLElement) = {
    val h = hourField.textContent.toInt
    val m = minuteField.textContent.toInt
    require(h inRange(0, 23))
    require(m inRange(0, 59))
    (startDay + (h hours) + (m minutes)).valueOf.toLong
  }

  def buildEvent = {
    val startDateMillis = getMillisFrom(startHourField, startMinuteField)
    val endDateMillis = getMillisFrom(endHourField, endMinuteField)

    new Event(
      id = EventId(-1),
      name = eventNameField.textContent,
      startDate = startDateMillis,
      endDate = endDateMillis,
      location = eventLocationField.textContent,
      url = eventUrlField.textContent
    )
  }

  def sendEvent(event: Event) = {
    showMessage("Submitting event, please wait...")

    octoApi.addEvent(event).call().foreach {
      case SuccessMessage() =>
        showMessage("Successfully added event.")
        timers.setTimeout(MoveToCalendarDelay)(EventCreateWindowOperations.closeWindow(octopusHome))
        timers.setTimeout(MoveToCalendarDelay + WindowOpenDelay) {
          CalendarWindowOperations.openCalendarWindow(octopusHome, new Date(event.startDate))
        }

      case Invalid(arg) =>
        showMessage(s"Invalid $arg")
        hide(submitButton)

      case _ =>
        println("Unknown message received")
        hide(submitButton)
    }
  }

  def submit(me: MouseEvent) =
    Try(buildEvent) match {
      case Success(event) => sendEvent(event)
      case Failure(thr) => showError(thr.toString)
    }

  def showMessage(text: String) = {
    messageField.textContent = text
    show(messageField)
  }

  def showError(message: String) = {
    val suffixes: List[String] = List(InvalidDatesMessage, InvalidNameMessage, InvalidLocationMessage, InvalidURLMessage)
    FindSuffixInMessage(message, suffixes) match {
      case SuffixFound(suffix) => showMessage(suffix)
      case _ if message.startsWith(classOf[NumberFormatException].getName) =>
        showMessage("We need to know when the event starts and ends!")
    }
  }

  def show(element: HTMLElement) = element.classList.remove("hidden")

  def hide(element: HTMLElement) = element.classList.add("hidden")
}

object FormHandler {
  def getNewTimeField(hint: String, min: Int, max: Int) =
    span(
      placeholder := hint,
      onkeypress := intOnlyKeyHandler(2, min, max) _,
      contenteditable := "true"
    ).render

  def intOnlyKeyHandler(maxLength: Int, min: Int, max: Int)(event: DomEvent): Unit = event match {
    case e: KeyboardEvent =>
      val oldContent = e.srcElement.textContent
      Try(e.charCode.toChar.toString.toInt) match {
        case f: Failure[_] =>
          event.preventDefault()
        case _ => timers.setTimeout(KeyCheckDelay) {
          if (e.srcElement.textContent.length > maxLength
            || !(e.srcElement.textContent.toInt inRange(min, max)))
            e.srcElement.textContent = oldContent
        }
      }
  }
}