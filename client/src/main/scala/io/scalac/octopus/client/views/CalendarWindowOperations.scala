package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.{DateOps, EventDateOps}
import org.scalajs.dom.html.Div

import scala.scalajs.js.{Date, timers}
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

object CalendarWindowOperations extends WindowOperations {
  type CalendarWindowOption = Option[Div]

  protected var calendarWindow: CalendarWindowOption = None

  /*todo replace events parameter with an API call to events in given month*/
  def openCalendarWindow(events: Array[Event])(octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    calendarWindow = switchCalendarWindow(events)(octopusHome)
  }

  def switchCalendarWindow(events: Array[Event])(octopusHome: Div): CalendarWindowOption =
    calendarWindow match {
      case Some(window) =>
        closeWindow(octopusHome)
        None
      case None =>
        import DateOps._
        import EventDateOps._

        val now = new Date(Date.now())

        val window: Div = div(
          div(),
          `class` := "octopus-window octopus-calendar closed",
          div(`class` := "octopus-window-bottom-arrow arrow-left")
        ).render

        object CalendarUtils {
          def calendarTable(currentMonth: Date): Div = CalendarTable(
            currentMonth,
            marker = calendarDay =>
              (calendarDay isSameMonth currentMonth) && events.exists(_ takesPlaceOn calendarDay),
            modifier = {
              case date if (date isSameMonth currentMonth) && events.exists(_ takesPlaceOn date) =>
                Array(span(date.getDate()),
                  div(
                    `class` := "octopus-preview",
                    events.filter(_ takesPlaceOn date).map { event =>
                      div(
                        event.name,
                        `class` := "octopus-preview-element",
                        onclick := { () =>
                          closeWindow(octopusHome)
                          timers.setTimeout(ClientConfig.WindowOpenDelay)(EventWindowOperations.openEventWindow(event)(octopusHome))
                        }
                      )
                    }
                  ))
              case date => Array(date.getDate().toString)
            }
          ).render

          def monthSelector(current: Date): Div = {
            val yearString = current.getFullYear().toString
            div(
              `class` := "octopus-calendar-arrow-wrapper",
              span(`class` := "octopus-calendar-arrow arrow-left", onclick := { () =>
                window.replaceChild(tableWithSelector(getPreviousMonth(current)), window.firstChild)
              }),
              span(Months(current.getMonth()) + " '" + yearString.substring(yearString.length - 2),
                `class` := "octopus-calendar-month"
              ),
              span(`class` := "octopus-calendar-arrow arrow-right", onclick := { () =>
                window.replaceChild(tableWithSelector(getNextMonth(current)), window.firstChild)
              })
            ).render
          }

          def tableWithSelector(current: Date) =
            div(
              `class` := "octopus-table-wrapper",
              monthSelector(current),
              calendarTable(current)
            ).render
        }

        window.replaceChild(CalendarUtils.tableWithSelector(now), window.firstChild)
        openWindow(window)(octopusHome)
        Option(window)
    }

  def closeWindow(octopusHome: Div): Unit = calendarWindow = calendarWindow match {
    case Some(openedWindow) =>
      removeWindow(openedWindow)(octopusHome)
      None
    case None => None
  }
}