package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.DateOps._
import io.scalac.octopus.client.tools.TimeUnit._
import io.scalac.octopus.client.tools.{DateOps, TimeUnit}
import org.scalajs.dom.html.Div

import scala.language.postfixOps
import scala.scalajs.js.Date
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

object CalendarTable {

  /**
   * Creates a calendar table including the current month (specified by the "now" date parameter).
   * It will add a "marked" class to every day for which the marker function returns true.
   **/
  def apply(now: Date, marker: Date => Boolean, modifier: Date => Array[Modifier]): TypedTag[Div] = {

    def classFor(day: Date): String = day match {
      case theDay if marker(theDay) => "marked"
      case theDay if theDay isSameDay new Date(Date.now()) => "current-day"
      case theDay if theDay isSameMonth now => "current-month"
      case _ => "other-month"
    }

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
        `class` := "row",
        dayNames.map(div(_, `class` := "cell"))
      ),

      weeks.map(week =>
        div(
          `class` := "row",
          week.map(day =>
            div(
              modifier(day),
              `class` := "cell " + classFor(day)
            ).render)
        )
      )
    )
  }
}
