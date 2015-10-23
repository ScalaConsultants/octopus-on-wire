package io.scalac.octopus.client

import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.Event

/**
 * @author kubukoz
 *         created on 22/10/15.
 */

class EventDateOps(event: Event) {
  val startDateFull = EventDateOps.dateToString(new Date(event.startDate))
  val endDateFull = EventDateOps.dateToString(new Date(event.endDate))

  def endDateHour = Some(new Date(event.endDate)).map(d => "%02d:%02d".format(d.getHours(), d.getMinutes())).get

  def datesToString = startDateFull + " - " + (if (days.size > 1) endDateFull else endDateHour)

  def days = (for {
    dayMs <- event.startDate to event.endDate by ClientConfig.SecondsInDay
    dayStart = EventDateOps.beginningOfDay(new Date(dayMs))
  } yield dayStart).distinct
}

object EventDateOps {
  val months = Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

  implicit def event2EventOps(e: Event): EventDateOps = new EventDateOps(e)

  def beginningOfDay(day: Date) = new Date(day.getFullYear(), day.getMonth(), day.getDate()).valueOf()

  def dateToString(d: Date) = "%s %d, %d %02d:%02d".format(months(d.getMonth()), d.getDate(), d.getFullYear(), d.getHours(), d.getMilliseconds())
}