package io.scalac.octopus.client.views.addition

import io.scalac.octopus.client.tools.DateOps.date2DateOps
import io.scalac.octopus.client.views.calendarview.{CalendarTable, CalendarViewTemplate}
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
        if (date isAfterOrOnToday)
          EventCreateWindowOperations.closeOthersAndOpenWindow(date, date, octopusHome)
      }
    )

    override def classMapper(date: Date): List[String] = date match {
      case day if day isBeforeOrOnToday => "inactive-day" :: Nil
      case _ => "clickable" :: super.classMapper(date)
    }
  }.view
}
