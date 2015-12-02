package io.scalac.octopus.client.views.calendarview

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import org.scalajs.dom.html.Div

import scala.language.{implicitConversions, postfixOps}
import scala.scalajs.js.Date
import scalatags.JsDom.all._

class CalendarTable(now: Date) {

  def marker(date: Date): Boolean = false

  def modifier(date: Date): Array[Modifier] = Array(date.getDate().toString)
  def classMapper(date: Date): String = classFor(date)

  /**
    * A calendar table including the current month (specified by the "now" date parameter).
    * It will add a "marked" class to every day for which the marker function returns true.
    **/
  val view: Div = {
    val monthStart = getMonthStart(now)
    val calendarStart = monthStart - (((monthStart.getDay() + 6) % 7) days)

    val weeks: Seq[Date] =
      for {
        wI <- 0 until ClientConfig.WeeksToDisplay
        w = wI * 7
        i <- w until w + 7
        day = getDayStart(calendarStart + (i days))
      } yield day

    val dayNames = for {
      i <- 0 until 7
      day = calendarStart + ((i - 1) days)
    } yield Days(day.getDay())

    div(
      `class` := "ttable",
      div(
        `class` := "trow",
        dayNames.map(div(_, `class` := "tcell"))
      ),

      weeks.grouped(7).map(week =>
        div(
          `class` := "trow",
          week.map(
            day => {
              val cell = div(modifier(day)).render

              //purposely add classes after modifier is called
              Array("tcell", classMapper(day)).foreach(cell.classList.add)
              cell
            }
          )
        )
      ).toList
    ).render
  }

  def classFor(day: Date): String = day match {
    case theDay if marker(theDay) => "marked"
    case theDay if theDay isSameDay new Date(Date.now()) => "current-day"
    case theDay if theDay isSameMonth now => "normal-day"
    case _ => "inactive-day"
  }
}