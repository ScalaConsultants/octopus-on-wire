package io.scalac.octopus.client.views.calendar

import autowire._
import boopickle.Default._
import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.config.ClientConfig.octoApi
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.EventDateOps._
import io.scalac.octopus.client.views.calendarview.{CalendarViewTemplate, CalendarTable}
import io.scalac.octopus.client.views.detail.EventDetailWindow
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

    new CalendarTable(now = current) {
      override def marker(date: Date): Boolean = events.exists(_ takesPlaceOn date)

      override def modifier(day: Date): Array[Modifier] = day match {
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
                      EventCalendarWindow.closeWindow(octopusHome)
                      timers.setTimeout(ClientConfig.WindowOpenDelay)(EventDetailWindow.open(event.id, octopusHome))
                    }
                  )
                }
              )
            ))
        case date => super.modifier(date)
      }
    }.view
  }
}

object EventCalendar {
  var current: Option[Date] = None
}