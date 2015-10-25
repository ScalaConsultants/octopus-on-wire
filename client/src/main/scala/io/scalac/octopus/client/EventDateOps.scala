package io.scalac.octopus.client

import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.Event

class EventDateOps(event: Event) {
  def startDateFull = EventDateOps.dateToString(new Date(event.startDate))
  def endDateFull = EventDateOps.dateToString(new Date(event.endDate))

  def endDateHour = {
    val date = new Date(event.endDate)
    "%02d:%02d".format(date.getHours(), date.getMinutes())
  }

  def datesToString = startDateFull + " - " + (if (days.size > 1) endDateFull else endDateHour)

  def days = for {
    dayMs <- event.startDate to event.endDate by TimeUnit.Day
  } yield new Date(dayMs)
}

object EventDateOps {
  import DateOps.MonthsShort

  implicit def event2EventOps(e: Event): EventDateOps = new EventDateOps(e)

  def beginningOfDay(day: Date) = new Date(day.getFullYear(), day.getMonth(), day.getDate())

  def dateToString(d: Date) = "%s %d, %d %02d:%02d".format(MonthsShort(d.getMonth()), d.getDate(), d.getFullYear(), d.getHours(), d.getMilliseconds())
}