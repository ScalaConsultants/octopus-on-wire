package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.OctopusClient
import io.scalac.octopus.client.config.ClientConfig.{KeyCheckDelay, MoveToCalendarDelay, WindowOpenDelay, octoApi}
import io.scalac.octopus.client.tools.DateOps
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.views.FormHandler.{MalformedDatesMessage, addValidationListeners, getNewTimeField}
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.html.{Button, Div, Span}
import org.scalajs.dom.raw.{FocusEvent, HTMLElement, MouseEvent, UIEvent}

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js.{Date, timers}
import scala.util.{Success, Try}
import scalac.octopusonwire.shared.domain.Event._
import scalac.octopusonwire.shared.domain._
import scalac.octopusonwire.shared.tools.LongRangeOps._
import scalatags.JsDom.all._

class FormHandler(startDay: Date, octopusHome: Div) {
  //text fields
  val eventNameField = addValidationListeners(h1(
    placeholder := "Event name",
    `class` := "octopus-event-name", contenteditable := "true"
  ).render, validateForm)

  val eventLocationField = addValidationListeners(
    p(placeholder := "Event location", contenteditable := "true").render,
    validateForm)

  val eventUrlField = addValidationListeners(p(
    placeholder := "Event url",
    contenteditable := "true",
    `class` := "octopus-event-url"
  ).render, validateForm)

  /* used with time fields that don't have their own FieldWithError instance.
   they call endMinuteField's instead */
  def validateFormAndCheckTimeErrors(): Unit = {
    endMinuteWrapper.makeDirty()
    validateForm()
  }

  //time fields
  val startHourField = getNewTimeField("H", 0, 23, maxLength = 2, validateFormAndCheckTimeErrors)
  val startMinuteField = getNewTimeField("MM", 0, 59, maxLength = 2, validateFormAndCheckTimeErrors)
  val endHourField = getNewTimeField("H", 0, 23, maxLength = 2, validateFormAndCheckTimeErrors)
  val endMinuteField = getNewTimeField("MM", 0, 59, maxLength = 2, validateForm)
  val timezoneHourField = span("H").render
  val timezoneMinuteField = span("MM").render

  //set initial timezone value based on the user's location
  val tz = new Date(Date.now).getTimezoneOffset
  val sign = if (tz >= 0) '-' else '+'
  timezoneHourField.textContent = sign + "%02d".format((tz / 60).abs)
  timezoneMinuteField.textContent = "%02d".format((-tz % 60).abs)

  val messagesWithFields: Map[String, HTMLElement] = Map(
    MalformedDatesMessage -> endMinuteField,
    InvalidNameMessage -> eventNameField,
    InvalidDatesMessage -> endMinuteField,
    //    InvalidOffsetMessage -> timezoneMinuteField,
    InvalidLocationMessage -> eventLocationField,
    InvalidURLMessage -> eventUrlField
  )

  //validated fields wrappers
  val eventNameWrapper = FieldWithErrorMessages(eventNameField, messagesWithFields)
  val eventLocationWrapper = FieldWithErrorMessages(eventLocationField, messagesWithFields)
  val eventUrlWrapper = FieldWithErrorMessages(eventUrlField, messagesWithFields)
  val endMinuteWrapper = FieldWithErrorMessages(endMinuteField, messagesWithFields)

  //and a list of them all
  val fieldsWithValidation = List(eventNameWrapper, eventLocationWrapper, eventUrlWrapper, endMinuteWrapper)

  //field for displaying results of sending the event to the server
  val messageField = p(`class` := "octopus-message hidden").render

  val submitButton: Button = button("Submit", `class` := "octopus-event-submit-button", onclick := submit _).render

  def view: List[HTMLElement] = List(
    eventNameWrapper.contents,
    p(DateOps.dateToString(startDay), `class` := "octopus-event-date").render,
    p(
      "From ",
      startHourField, ":",
      startMinuteField, " to ",
      endHourField, ":", endMinuteWrapper.contents,
      " Timezone: UTC ", timezoneHourField, ":", timezoneMinuteField
    ).render,
    eventLocationWrapper.contents,
    eventUrlWrapper.contents,
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

  def buildEventOrGetErrors: Either[Event, Set[String]] = {
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

    //optional error caused by bad time format (e.g. empty time fields)
    val timeFormatResult: Set[String] =
      if (millis.isSuccess) Set.empty
      else Set(MalformedDatesMessage)

    val messagesToShow = invalidFieldsIn(baseEvent) ++ timeFormatResult
    if (messagesToShow.isEmpty) Left(from(baseEvent))
    else Right(messagesToShow)
  }

  //sends valid event to the server
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

  def submit(me: MouseEvent) = {
    buildEventOrGetErrors match {
      case Left(event) => sendEvent(from(event))
      case Right(errors) =>
        fieldsWithValidation.foreach(_.makeDirty())
        showErrors(errors)
    }
  }

  def showMessage(text: String) = {
    messageField.textContent = text
    show(messageField)
  }

  def validateForm() =
    buildEventOrGetErrors match {
      case Right(errors) => showErrors(errors)
      case _ => showErrors(Set.empty)
    }

  def showErrors(errors: Set[String]): Unit =
    fieldsWithValidation.foreach(_.updateErrors(errors))

  def show(element: HTMLElement) = element.classList.remove("hidden")
}

object FormHandler {
  val MalformedDatesMessage = "We need to know when the event starts and ends!"

  def getNewTimeField(hint: String, min: Int, max: Int, maxLength: Int,
                      validationCallback: () => Unit): Span = {
    val field = span(placeholder := hint, contenteditable := "true").render
    val handler = new IntOnlyKeyHandler(maxLength, min, max, field, validationCallback)
    field.onkeypress = (event: KeyboardEvent) => {
      handler.handleEvent(event)
    }
    field.onblur = (event: FocusEvent) => {
      validationCallback()
    }
    field
  }

  //adds keypress / focusleave listeners that call specified callback
  def addValidationListeners[T <: HTMLElement](field: T, callback: () => Unit): T = {
    val listener = (_: UIEvent) => {
      field.parentElement.classList add "dirty"
      timers.setTimeout(KeyCheckDelay)(callback())
    }

    field.onkeypress = listener
    field.onblur = listener

    field
  }
}

class IntOnlyKeyHandler(maxLength: Int, min: Int, max: Int, view: HTMLElement, validationCallback: () => Unit) {
  var lastValidValue = view.textContent

  def handleValidKey(ch: Char): Unit =
    timers.setTimeout(KeyCheckDelay) {
      if (view.textContent.length > maxLength || !(view.textContent.toInt inRange(min, max)))
        view.textContent = lastValidValue
      else
        lastValidValue = view.textContent
      validationCallback()
    }

  def handleEvent(e: KeyboardEvent) = {
    val ch = e.charCode.toChar
    Try(ch.toString.toInt) match {
      case Success(_) => handleValidKey(ch)
      // case Failure(_) if List('-', '+') contains ch => handleValidKey(ch)
      case _ =>
        //called here because the first case runs a delayed operation
        validationCallback()
        e.preventDefault()
    }
  }
}

case class FieldWithErrorMessages[T <: HTMLElement] private(field: T, messageSource: Map[String, HTMLElement]) {
  val messages = messageSource.filter(_._2 == field).keySet

  def updateErrors(errors: Set[String]) = {
    messages.zipWithIndex.partition(errors contains _._1) match {
      case (invalid, valid) =>
        invalid.foreach { case (_, index) => contents.children(1 + index).classList add "invalid" }
        valid foreach { case (_, index) => contents.children(1 + index).classList remove "invalid" }
    }
  }

  def getText = field.textContent

  def makeDirty() = contents.classList add "dirty"

  private val wrapperClass = Option(field.className).filterNot(_ == "").map(" " + _ + "-wrapper")

  val contents = div(
    `class` := "octopus-validation-parent" + wrapperClass.getOrElse(""),
    field,
    messages.map { message =>
      span(`class` := "octopus-validation-message", "!", title := message).render
    }.toList
  ).render
}