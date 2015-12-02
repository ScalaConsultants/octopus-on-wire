package io.scalac.octopus.client.views.calendarview

import io.scalac.octopus.client.tools.DateOps._
import org.scalajs.dom.html.Div

import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._
abstract class CalendarViewTemplate(window: Div, octopusHome: Div) {

  def calendarTable(current: Date, events: Seq[Event]): Div = new CalendarTable(current).view

  def apply(current: Date): Div = {
    val monthSelector: Div = {
      val yearString = current.getFullYear().toString
      div(
        `class` := "octopus-calendar-arrow-wrapper",
        span(`class` := "octopus-calendar-arrow arrow-left", onclick := { () =>
          window.replaceChild(apply(getPreviousMonthStart(current)), window.firstChild)
        }),
        span(Months(current.getMonth()) + " '" + yearString.substring(yearString.length - 2),
          `class` := "octopus-calendar-month"
        ),
        span(`class` := "octopus-calendar-arrow arrow-right", onclick := { () =>
          window.replaceChild(apply(getNextMonthStart(current)), window.firstChild)
        })
      ).render
    }

    val view = div(
      `class` := "octopus-table-wrapper",
      monthSelector,
      calendarTable(current, Array.empty[Event])
    ).render

    view
  }
}