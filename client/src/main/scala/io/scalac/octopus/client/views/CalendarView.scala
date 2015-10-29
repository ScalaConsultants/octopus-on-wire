package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.EventDateOps._
import org.scalajs.dom.html.Div

import scala.scalajs.js.{Date, timers}
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

class CalendarView(window: Div, octopusHome: Div) {
  def apply(current: Date, events: Array[Event]): Div = {
    val calendarTable: Div = CalendarTable(
      current,
      marker = calendarDay =>
        (calendarDay isSameMonth current) && events.exists(_ takesPlaceOn calendarDay),
      modifier = {
        case date if (date isSameMonth current) && events.exists(_ takesPlaceOn date) =>
          Array(span(date.getDate()),
            div(
              `class` := "octopus-preview",
              div(
                `class` := "octopus-preview-elements",
                events.filter(_ takesPlaceOn date).map { event =>
                  div(
                    event.name,
                    `class` := "octopus-preview-element",
                    onclick := { () =>
                      CalendarWindowOperations.closeWindow(octopusHome)
                      timers.setTimeout(ClientConfig.WindowOpenDelay)(EventWindowOperations.openEventWindow(event)(octopusHome))
                    }
                  )
                }
              )
            ))
        case date => Array(date.getDate().toString)
      }
    ).render

    val monthSelector: Div = {
      val yearString = current.getFullYear().toString
      div(
        `class` := "octopus-calendar-arrow-wrapper",
        span(`class` := "octopus-calendar-arrow arrow-left", onclick := { () =>
          window.replaceChild(apply(getPreviousMonth(current), events), window.firstChild)
        }),
        span(Months(current.getMonth()) + " '" + yearString.substring(yearString.length - 2),
          `class` := "octopus-calendar-month"
        ),
        span(`class` := "octopus-calendar-arrow arrow-right", onclick := { () =>
          window.replaceChild(apply(getNextMonth(current), events), window.firstChild)
        })
      ).render
    }

    div(
      `class` := "octopus-table-wrapper",
      monthSelector,
      calendarTable
    ).render
  }
}
