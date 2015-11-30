package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.OctopusClient
import io.scalac.octopus.client.config.ClientConfig.{KeyCheckDelay, MoveToCalendarDelay, WindowOpenDelay, octoApi}
import io.scalac.octopus.client.tools.DateOps
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.views.FormHandler.getNewTimeField
import org.scalajs.dom.html.{Button, Div}
import org.scalajs.dom.raw.{HTMLElement, MouseEvent}
import org.scalajs.dom.{Event => DomEvent, KeyboardEvent}

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.{Date, timers}
import scala.util.{Success, Try}
import scalac.octopusonwire.shared.domain.Event._
import scalac.octopusonwire.shared.domain._
import scalac.octopusonwire.shared.tools.LongRangeOps._
import scalatags.JsDom.all._

class FormHandler(startDay: Date, octopusHome: Div) {
  var submitted = false
  val MalformedDatesMessage = "We need to know when the event starts and ends!"

  //todo map contenteditable fields' onkeypressed to buildEventOrGetErrors

  val eventNameField = h1(
    placeholder := "Event name",
    `class` := "octopus-event-name", contenteditable := "true"
  ).render

  val startHourField = getNewTimeField("H", 0, 23, maxLength = 2)
  val startMinuteField = getNewTimeField("MM", 0, 59, maxLength = 2)
  val endHourField = getNewTimeField("H", 0, 23, maxLength = 2)
  val endMinuteField = getNewTimeField("MM", 0, 59, maxLength = 2)
  val timezoneHourField = span("H").render
  val timezoneMinuteField = span("MM").render

  //set initial timezone value based on the user's location
  val tz = new Date(Date.now).getTimezoneOffset
  val sign = if (tz >= 0) '-' else '+'
  timezoneHourField.textContent = sign + "%02d".format((tz / 60).abs)
  timezoneMinuteField.textContent = "%02d".format((-tz % 60).abs)

  val eventLocationField = p(placeholder := "Event location", contenteditable := "true").render

  val eventUrlField = p(
    placeholder := "Event url",
    contenteditable := "true",
    `class` := "octopus-event-url"
  ).render

  val messagesWithFields: Map[String, HTMLElement] = Map(
    MalformedDatesMessage -> endMinuteField,
    InvalidNameMessage -> eventNameField,
    InvalidDatesMessage -> endMinuteField,
    InvalidOffsetMessage -> timezoneMinuteField,
    InvalidLocationMessage -> eventLocationField,
    InvalidURLMessage -> eventUrlField
  )

  val messageField = p(`class` := "octopus-message hidden").render

  val submitButton: Button = button("Submit", `class` := "octopus-event-submit-button", onclick := submit _).render

  def view: List[HTMLElement] = List(
    eventNameField,
    p(DateOps.dateToString(startDay), `class` := "octopus-event-date").render,
    p(
      "From ",
      startHourField, ":",
      startMinuteField, " to ",
      endHourField, ":", endMinuteField,
      " Timezone: UTC ", timezoneHourField, ":", timezoneMinuteField
    ).render,
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

  def buildEventOrGetErrors: Either[Event, Map[String, HTMLElement]] = {
    val millis = Try {
      getMillisFrom(startHourField, startMinuteField) -> getMillisFrom(endHourField, endMinuteField)
    }

    val tzOffset = ((timezoneHourField.textContent.toInt hours)
      + (timezoneMinuteField.textContent.toInt minutes))
      .valueOf().toLong

    val (startDateMillis, endDateMillis) = millis.getOrElse((-1L, 0L))

    val baseEvent = new BaseEvent(
      id = EventId(-1),
      name = eventNameField.textContent,
      startDate = startDateMillis,
      endDate = endDateMillis,
      offset = tzOffset,
      url = eventUrlField.textContent,
      location = eventLocationField.textContent
    )
    val timeFormatResult: Map[String, Boolean] = if (millis.isSuccess) Map.empty
    else Map(MalformedDatesMessage -> false)

    val messagesToShow = (invalidFieldsIn(baseEvent) ++ timeFormatResult)
      .map { case (message, _) => message -> messagesWithFields(message) }

    if (messagesToShow.isEmpty) Left(from(baseEvent))
    else Right(messagesToShow)
  }

  def sendEvent(event: Event) = {
    showMessage("Submitting event, please wait...")

    octoApi.addEvent(event).call().foreach {
      case Added() =>
        showMessage(s"Your Event ${event.name} has been created.")

        OctopusClient.refreshEvents(SliderViewOperations.list, octopusHome)
        timers.setTimeout(MoveToCalendarDelay)(EventCreateWindowOperations.closeWindow(octopusHome))
        timers.setTimeout(MoveToCalendarDelay + WindowOpenDelay) {
          CalendarWindowOperations.openCalendarWindow(octopusHome, new Date(event.startDate))
        }

      case FailedToAdd(arg) =>
        showMessage(arg)
        show(submitButton)

      case _ =>
        println("Unknown message received")
        show(submitButton)
    }
  }

  def submit(me: MouseEvent) =
    buildEventOrGetErrors match {
      case Left(event) => sendEvent(from(event))
      case Right(errors) => /*todo show errors*/ errors foreach println
    }

  def showMessage(text: String) = {
    messageField.textContent = text
    show(messageField)
  }

  def show(element: HTMLElement) = element.classList.remove("hidden")

  def hide(element: HTMLElement) = element.classList.add("hidden")
}

object FormHandler {
  def getNewTimeField(hint: String, min: Int, max: Int, maxLength: Int) = {
    val field = span(
      placeholder := hint,
      contenteditable := "true"
    ).render
    field.onkeypress = new IntOnlyKeyHandler(maxLength, min, max, field).handleEvent _
    field
  }
}

class IntOnlyKeyHandler(maxLength: Int, min: Int, max: Int, view: HTMLElement) {
  var lastValidValue = view.textContent

  def handleValidKey(ch: Char): Unit =
    timers.setTimeout(KeyCheckDelay) {
      if (view.textContent.length > maxLength || !(view.textContent.toInt inRange(min, max)))
        view.textContent = lastValidValue
      else
        lastValidValue = view.textContent
    }

  def handleEvent(event: DomEvent) = event match {
    case e: KeyboardEvent =>
      val ch = e.charCode.toChar
      Try(ch.toString.toInt) match {
        case Success(_) => handleValidKey(ch)
        // case Failure(_) if List('-', '+') contains ch => handleValidKey(ch)
        case _ => event.preventDefault()
      }
  }
}