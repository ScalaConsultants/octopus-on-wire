package io.scalac.octopus.client.views

import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.config.ClientConfig.octoApi
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.EventDateOps._
import org.scalajs.dom.html.Div
import concurrent.ExecutionContext.Implicits.global
import autowire._
import scala.scalajs.js.{Date, timers}
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

class CalendarView(window: Div, octopusHome: Div) {
  def apply(current: Date): Div = {

    def calendarTable(events: Seq[Event]): Div = CalendarTable(
      current,
      marker = calendarDay =>
        events.exists(_ takesPlaceOn calendarDay),
      modifier = {
        case date if events.exists(_ takesPlaceOn date) =>
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
                      timers.setTimeout(ClientConfig.WindowOpenDelay)(EventWindowOperations.openEventWindow(event.id, octopusHome))
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
          window.replaceChild(apply(getPreviousMonth(current)), window.firstChild)
        }),
        span(Months(current.getMonth()) + " '" + yearString.substring(yearString.length - 2),
          `class` := "octopus-calendar-month"
        ),
        span(`class` := "octopus-calendar-arrow arrow-right", onclick := { () =>
          window.replaceChild(apply(getNextMonth(current)), window.firstChild)
        })
      ).render
    }

    val view = div(
      `class` := "octopus-table-wrapper",
      monthSelector,
      calendarTable(Array.empty[Event])
    ).render

    val monthStart: Long = getMonthStart(current).valueOf().toLong
    val monthEnd: Long = getMonthEnd(current).valueOf().toLong

    octoApi.getEventsForRange(monthStart, monthEnd).call().foreach{ monthEvents =>
      view.replaceChild(calendarTable(monthEvents), view.lastChild)
    }

    view
  }
}
