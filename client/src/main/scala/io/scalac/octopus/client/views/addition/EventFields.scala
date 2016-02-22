package io.scalac.octopus.client.views.addition

import boopickle.Default._
import io.scalac.octopus.client.tools.DateOps
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.views.addition.FieldWithErrorMessages._
import io.scalac.octopus.client.views.addition.IntOnlyKeyHandler._
import org.scalajs.dom.raw.HTMLElement

import scala.language.postfixOps
import scala.scalajs.js.Date
import scalac.octopusonwire.shared.tools.LongRangeOps._
import scalatags.JsDom.all._

trait EventFields {
  self: EventAdditionForm =>

  //text fields
  val eventNameField = addValidationListeners(h1(
    placeholder := "Event name",
    `class` := "octopus-event-name", contenteditable := "true"
  ).render, validateForm)

  val eventLocationField = addValidationListeners(
    p(placeholder := "Event location", contenteditable := "true").render,
    validateForm)

  val eventUrlField = addValidationListeners(p(
    placeholder := "Event URL",
    contenteditable := "true",
    `class` := "octopus-event-url"
  ).render, validateForm)

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

  def view: List[HTMLElement] = List(
    eventNameWrapper.contents,
    p(DateOps.dateToString(startDay), `class` := "octopus-event-date").render,
    p(
      "From ",
      startHourField, ":",
      startMinuteField, " to ",
      endHourField, ":", endMinuteWrapper.contents,
      " Timezone: UTC ", timezoneHourField, ":", timezoneMinuteField,
      `class` := "octopus-event-hours"
    ).render,
    eventLocationWrapper.contents,
    eventUrlWrapper.contents,
    messageField,
    submitButton
  )

  def getMillisFrom(day: Date, hourField: HTMLElement, minuteField: HTMLElement) = {
    val h = hourField.textContent.toInt
    val m = minuteField.textContent.toInt
    require(h inRange(0, 23))
    require(m inRange(0, 59))
    (day + (h hours) + (m minutes)).valueOf.toLong
  }

  def show(element: HTMLElement) = {
    element.classList remove "hidden"
  }

  def showWithText(element: HTMLElement, text: String) = {
    element.textContent = text
    element.classList remove "hidden"
  }

  def hide(element: HTMLElement) = element.classList add "hidden"
}
