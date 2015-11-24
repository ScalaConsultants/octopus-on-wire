package io.scalac.octopus.client.tools

import scala.language.implicitConversions
import scala.language.postfixOps
import scala.scalajs.js.Date
import scalac.octopusonwire.shared.domain.Event

import DateOps._

class EventDateOps(event: Event) {
  def startDateFull = DateOps.dateAndTimeToString(new Date(event.startDate))
  def endDateFull = DateOps.dateAndTimeToString(new Date(event.endDate))

  def endDateHour = {
    val date = new Date(event.endDate)
    "%02d:%02d".format(date.getHours(), date.getMinutes())
  }

  def datesToString = startDateFull + " - " + (if (days.size > 1) endDateFull else endDateHour)

  def days = for {
    dayMs <- event.startDate to event.endDate by TimeUnit.Day
  } yield new Date(dayMs)

  def takesPlaceOn(day: Date) = days.exists(day isSameDay)
}

object EventDateOps {
  implicit def event2EventOps(e: Event): EventDateOps = new EventDateOps(e)
}