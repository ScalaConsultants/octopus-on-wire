package io.scalac.octopus.client.views.addition

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.OctopusClient
import io.scalac.octopus.client.config.ClientConfig.{MoveToCalendarDelay, WindowOpenDelay, octoApi}
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.views.SliderViewOperations
import io.scalac.octopus.client.views.calendar.EventCalendarWindow
import org.scalajs.dom.html.{Button, Div}
import org.scalajs.dom.raw.MouseEvent

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.{Date, timers}
import scala.util.Try
import scalac.octopusonwire.shared.domain.Event._
import scalac.octopusonwire.shared.domain._
import scalatags.JsDom.all._

class EventAdditionForm(val startDay: Date, octopusHome: Div) extends EventFields with ErrorMessages {

  //field for displaying results of sending the event to the server
  val messageField = p(`class` := "octopus-message hidden").render

  val submitButton: Button = button("Submit", `class` := "octopus-event-submit-button", onclick := submit _).render

  def buildEventOrGetErrors: Either[Event, Set[String]] = {
    val millis = Try {
      getMillisFrom(startDay, startHourField, startMinuteField) -> getMillisFrom(startDay, endHourField, endMinuteField)
    }

    val tzOffset = ((timezoneHourField.textContent.toInt hours)
      + (timezoneMinuteField.textContent.toInt minutes))
      .valueOf().toLong

    val (startDateMillis, endDateMillis) = millis.getOrElse((-1L, 0L))

    val baseEvent = new BaseEvent(
      id = NoId,
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
  def sendEvent(event: Event): Unit = {
    showWithText(messageField, "Submitting event, please wait...")
    hide(submitButton)
    octoApi.addEvent(event).call().foreach {
      case Added() =>
        showWithText(messageField, s"Your Event ${event.name} has been created.")

        OctopusClient.refreshEvents(SliderViewOperations.list)
        timers.setTimeout(MoveToCalendarDelay)(EventCreateWindowOperations.closeWindow(octopusHome))
        timers.setTimeout(MoveToCalendarDelay + WindowOpenDelay) {
          EventCalendarWindow.open(octopusHome, new Date(event.startDate))
        }

      case FailedToAdd(arg) =>
        showWithText(messageField, arg)
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
}