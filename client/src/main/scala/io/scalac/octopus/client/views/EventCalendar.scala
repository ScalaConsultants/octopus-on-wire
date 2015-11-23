package io.scalac.octopus.client.views

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.config.ClientConfig.octoApi
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.EventDateOps._
import org.scalajs.dom.html.Div

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.{Date, timers}
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

class EventCalendar(window: Div, octopusHome: Div) extends CalendarViewTemplate(window, octopusHome) {
  override def apply(current: Date): Div = {
    val view = super.apply(current)

    val monthStart: Long = getMonthStart(current).valueOf().toLong
    val monthEnd: Long = getMonthEnd(current).valueOf().toLong

    octoApi.getEventsForRange(monthStart, monthEnd).call().foreach { monthEvents =>
      view.replaceChild(calendarTable(current, monthEvents), view.lastChild)
    }

    view
  }

  override def calendarTable(current: Date, events: Seq[Event]): Div = {
    EventCalendar.current = Option(current)

    new CalendarTable(
      now = current,
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
        case date => CalendarTable.defaultModifier(date)
      }
    ).render

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

    val monthStart: Long = getMonthStart(current).valueOf().toLong
    val monthEnd: Long = getMonthEnd(current).valueOf().toLong

    octoApi.getEventsForRange(monthStart, monthEnd).call().foreach { monthEvents =>
      view.replaceChild(calendarTable(current, monthEvents), view.lastChild)
    }
  }
}
object EventCalendar {
  var current: Option[Date] = None
}