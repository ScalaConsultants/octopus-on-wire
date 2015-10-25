package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.{DateOps, EventDateOps}
import org.scalajs.dom.html.{Div, Table, UList}

import scala.scalajs.js.{Date, timers}
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._

object CalendarWindowOperations extends WindowOperations {
  type CalendarWindowOption = Option[Div]

  protected var calendarWindow: CalendarWindowOption = None
  val eventShortlist: UList = ul(`class` := "octopus-preview").render

  def clearShortList() =
    while (eventShortlist.hasChildNodes())
      eventShortlist.removeChild(eventShortlist.lastChild)

  /*todo replace events parameter with an API call to events in given month*/
  def openCalendarWindow(events: Array[Event])(implicit octopusHome: Div): Unit = {
    EventWindowOperations.closeWindow(octopusHome)
    calendarWindow = switchCalendarWindow(events)
  }

  def switchCalendarWindow(events: Array[Event])(implicit octopusHome: Div): CalendarWindowOption =
    calendarWindow match {
      case Some(window) =>
        closeWindow
        None
      case None =>
        import DateOps._
        import EventDateOps._

        val now = new Date(Date.now())
        val selectedDate = new Date(now.getFullYear(), now.getMonth(), 1)

        val window: Div = div(
          div(),
          eventShortlist,
          `class` := "octopus-window octopus-calendar closed",
          div(`class` := "octopus-window-bottom-arrow arrow-left")
        ).render

        object CalendarUtils {
          def calendarTable(currentMonth: Date): Table = CalendarTable(
            currentMonth,
            marker = calendarDay =>
              events.exists(_ takesPlaceOn calendarDay),
            clickListener = { date =>
              clearShortList()
              events.filter(_ takesPlaceOn date).foreach(event =>
                eventShortlist.appendChild(
                  li(
                    event.name,
                    `class` := "octopus-preview-element",
                    onclick := { () =>
                      closeWindow
                      timers.setTimeout(ClientConfig.WindowOpenDelay) {
                        EventWindowOperations.openEventWindow(event)
                      }
                    }
                  ).render
                )
              )
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

        window.replaceChild(CalendarUtils.tableWithSelector(selectedDate), window.firstChild)
        openWindow(window)
        Option(window)
    }

  def closeWindow(implicit octopusHome: Div): Unit = calendarWindow = calendarWindow match {
    case Some(openedWindow) =>
      removeWindow(openedWindow)
      timers.setTimeout(ClientConfig.WindowLoadTime)(clearShortList())
      None
    case None => None
  }
}

