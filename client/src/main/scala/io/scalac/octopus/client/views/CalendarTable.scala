package io.scalac.octopus.client.views

import io.scalac.octopus.client.config.ClientConfig
import io.scalac.octopus.client.tools.{DateOps, TimeUnit}

import scala.scalajs.js.Date
import scalatags.JsDom.all._

object CalendarTable {

  /**
   * Creates a calendar table including the current month (specified by the "now" date parameter).
   * It will add a "marked" class to every day for which the marker function returns true.
   **/
  def apply(now: Date, marker: Date => Boolean, clickListener: Date => Unit = { d => () }) = {
    import DateOps._
    import TimeUnit._

    import scala.language.postfixOps

    val monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
    val calendarStart = monthStart - (((monthStart.getDay() + 6) % 7) days)

    val weeksAndDays = for {
      wI <- 0 until ClientConfig.WeeksToDisplay
      w = wI * 7
    } yield {
        for {
          i <- w until w + 7
          day = calendarStart + (i days)
        } yield day
      }

    val dayNames = for {
      i <- 0 until 7
      day = calendarStart + ((i - 1) days)
    } yield Days(day.getDay())

    table(
      tr(dayNames.map(td(_))),

      weeksAndDays.map(wad =>
        tr(
          wad.map(day =>
            td(
              day.getDate(),
              `class` := (day match {
                case theDay if marker(theDay) => "marked"
                case theDay if theDay isSameDay new Date(Date.now()) => "current-day"
                case theDay if theDay isSameMonth now => "current-month"
                case _ => "other-month"
              }),
              onclick := { () => clickListener(day) }
            ).render)
        )
      )
    )
  }
}
