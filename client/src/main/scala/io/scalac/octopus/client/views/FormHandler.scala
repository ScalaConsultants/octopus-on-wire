package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig.{KeyCheckDelay, WindowOpenDelay, MoveToCalendarDelay, octoApi}
import scalac.octopusonwire.shared.tools.IntRangeOps.int2IntRangeOps
import io.scalac.octopus.client.tools.DateOps
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.views.FormHandler.getNewTimeField
import org.scalajs.dom.html.{Button, Div}
import org.scalajs.dom.raw.{HTMLElement, MouseEvent}
import org.scalajs.dom.{Event, KeyboardEvent}

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.{Date, timers}
import scala.util.{Failure, Try}
import scalac.octopusonwire.shared.domain
import scalac.octopusonwire.shared.domain.{EventId, Invalid, Success}
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
    p("From ", startHourField, ":", startMinuteField, " to ", endHourField, ":", endMinuteField).render,
    eventLocationField,
    eventUrlField,
    messageField,
    submitButton
  )

  def buildEvent = {
    val (sH, sM, eH, eM) = (startHourField.textContent.toInt, startMinuteField.textContent.toInt,
      endHourField.textContent.toInt, endMinuteField.textContent.toInt)

    require(sH inRange(0, 23))
    require(eH inRange(0, 23))
    require(sM inRange(0, 59))
    require(sM inRange(0, 59))

    val startMillis = (startDay
      + (sH hours)
      + (sM minutes)).valueOf.toLong

    val endMillis = (startDay
      + (eH hours)
      + (eM minutes)).valueOf.toLong

    new domain.Event(
      id = EventId(-1),
      name = eventNameField.textContent,
      startDate = startMillis,
      endDate = endMillis,
      location = eventLocationField.textContent,
      url = eventUrlField.textContent
    )
  }

  def sendEvent(event: domain.Event) = {
    showMessage("Submitting event, please wait...")

    octoApi.addEvent(event).call().foreach {
      case Success() =>
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
    Try(buildEvent)
      .map(sendEvent)
      .getOrElse(showMessage("Please fill in all required fields properly."))


  def showMessage(text: String) = {
    messageField.textContent = text
    show(messageField)
  }

  def show(element: HTMLElement) = element.classList.remove("hidden")

  def hide(element: HTMLElement) = element.classList.add("hidden")
}

object FormHandler {
  def getNewTimeField(hint: String, min: Int, max: Int) = span(placeholder := hint, contenteditable := "true", onkeypress := intOnlyKeyHandler(2, (min, max)) _).render

  def intOnlyKeyHandler(maxLength: Int, range: (Int, Int))(event: Event): Unit = event match {
    case e: KeyboardEvent =>
      val oldContent = e.srcElement.textContent
      Try(e.charCode.toChar.toString.toInt) match {
        case f: Failure[_] =>
          event.preventDefault()
        case _ => timers.setTimeout(KeyCheckDelay) {
          if (e.srcElement.textContent.length > maxLength || !(e.srcElement.textContent.toInt inRange(range._1, range._2)))
            e.srcElement.textContent = oldContent
        }
      }
  }
}