package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.views.CalendarTable._
import org.scalajs.dom.html.Div
import scala.language.{implicitConversions, postfixOps}
import scala.scalajs.js.Date
import scalatags.JsDom.all._

class CalendarTable(now: Date, marker: Marker = defaultMarker, modifier: CellModifier = defaultModifier) {

  /**
    * A calendar table including the current month (specified by the "now" date parameter).
    * It will add a "marked" class to every day for which the marker function returns true.
    **/
  val view: Div = {
    val monthStart = getMonthStart(now)
    val calendarStart = monthStart - (((monthStart.getDay() + 6) % 7) days)

    val weeks: Seq[Seq[Date]] =
      for {
        wI <- 0 until ClientConfig.WeeksToDisplay
        w = wI * 7
        week = for {
          i <- w until w + 7
          day = calendarStart + (i days)
        } yield day
      } yield week

    val dayNames = for {
      i <- 0 until 7
      day = calendarStart + ((i - 1) days)
    } yield Days(day.getDay())

    div(
      `class` := "table",
      div(
        `class` := "trow",
        dayNames.map(div(_, `class` := "tcell"))
      ),

      weeks.map(week =>
        div(
          `class` := "trow",
          week.map(
            day => {
              val cell = div(modifier(day)).render

              //purposely add classes after modifier is called
              Array("tcell", classFor(day)).foreach(cell.classList.add)
              cell
            }
          )
        )
      )
    ).render
  }

  def classFor(day: Date): String = day match {
    case theDay if marker(theDay) => "marked"
    case theDay if theDay isSameDay new Date(Date.now()) => "current-day"
    case theDay if theDay isSameMonth now => "current-month"
    case _ => "other-month"
  }
}

object CalendarTable {
  type Marker = Date => Boolean
  type CellModifier = Date => Array[Modifier]

  val defaultMarker: Marker = _ => false
  val defaultModifier: CellModifier = date => Array(date.getDate().toString)

  implicit def calendarTable2Div(calendarTable: CalendarTable): Div = calendarTable.view
}
