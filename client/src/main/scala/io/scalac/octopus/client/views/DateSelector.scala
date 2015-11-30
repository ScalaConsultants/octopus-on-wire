package io.scalac.octopus.client.views

import io.scalac.octopus.client.tools.DateOps.date2DateOps
import org.scalajs.dom.html.Div

import scala.language.postfixOps
import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.Event
import scalatags.JsDom.all._


class DateSelector(window: Div, octopusHome: Div) extends CalendarViewTemplate(window, octopusHome) {
  override def calendarTable(current: Date, events: Seq[Event]): Div = new CalendarTable(current) {
    override def modifier(date: Date) = Array(
      `class` := "octopus-selectable-day",
      super.modifier(date),
      onclick := { () =>
        if(date isAfterOrOnToday)
          EventCreateWindowOperations.closeOthersAndOpenWindow(date, date, octopusHome)
      }
    )

    override def classMapper(date: Date): String = date match{
      case day if day isBeforeOrOnToday => "inactive-day"
      case _ => super.classMapper(date)
    }
  }.view
}
