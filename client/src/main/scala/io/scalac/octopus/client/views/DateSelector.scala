package io.scalac.octopus.client.views

import org.scalajs.dom.html.Div

import scala.scalajs.js.Date
import scalatags.JsDom.all._
import scalac.octopusonwire.shared.domain.Event


class DateSelector(window: Div, octopusHome: Div) extends CalendarViewTemplate(window, octopusHome) {
  override def calendarTable(current: Date, events: Seq[Event]): Div = new CalendarTable(
    current,
    modifier = date => {
      Array(
        `class` := "octopus-selectable-day",
        CalendarTable.defaultModifier(date),
        onclick := { () =>
          EventCreateWindowOperations.closeOthersAndOpenWindow(date, date, octopusHome)
        }
      )
    }
  )
}
