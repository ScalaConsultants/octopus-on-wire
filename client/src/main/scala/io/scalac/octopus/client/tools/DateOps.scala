package io.scalac.octopus.client.tools

import io.scalac.octopus.client.tools.DateOps.{date2DateOps, getOffsetDifference}
import io.scalac.octopus.client.tools.TimeUnit._

import scala.language.{implicitConversions, postfixOps}
import scala.scalajs.js.Date

object DateOps {
  implicit def date2DateOps(d: Date): DateOps = new DateOps(d)

  val MonthsShort = Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
  val Months = Array("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
  val Days = Array("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

  def getMonthStart(now: Date): Date = new Date(now.getFullYear(), now.getMonth(), 1)

  def getMonthEnd(now: Date): Date = getNextMonthStart(getMonthStart(now)) - (1 seconds)

  def getNextMonthStart(currentMonth: Date): Date = {
    val willPassYear = currentMonth.getMonth() == 11
    val currentYear = currentMonth.getFullYear()
    new Date(
      year = if (willPassYear) currentYear + 1 else currentYear,
      month = if (willPassYear) 0 else currentMonth.getMonth() + 1,
      date = 1
    )
  }

  def getPreviousMonthStart(currentMonth: Date): Date = {
    val willPassYear = currentMonth.getMonth() == 0
    val currentYear = currentMonth.getFullYear()
    new Date(
      year = if (willPassYear) currentYear - 1 else currentYear,
      month = if (willPassYear) 11 else currentMonth.getMonth() - 1,
      date = 1
    )
  }

  def getOffsetDifference(first: Date, second: Date) = (first.getTimezoneOffset() - second.getTimezoneOffset()) * MillisecondsInMinute

  def getDayStart(day: Date) = new Date(day.getFullYear(), day.getMonth(), day.getDate())

  def dateAndTimeToString(d: Date) = "%s %d, %d %02d:%02d".format(MonthsShort(d.getMonth()), d.getDate(), d.getFullYear(), d.getHours(), d.getMinutes())

  def dateToString(d: Date) = "%s %d, %d".format(MonthsShort(d.getMonth()), d.getDate(), d.getFullYear())
}

class DateOps(date: Date) {
  def +(another: Date) = {
    val temp = new Date(date.valueOf() + another.valueOf())
    new Date(temp.valueOf() - getOffsetDifference(date, another))
  }

  def -(another: Date) = {
    val temp = new Date(date.valueOf() - another.valueOf())
    new Date(temp.valueOf() - getOffsetDifference(date, another))
  }

  def isSameYear(another: Date) =
    date.getFullYear() == another.getFullYear()

  def isSameMonth(another: Date) =
    isSameYear(another) && date.getMonth() == another.getMonth()

  def isSameDay(another: Date) =
    isSameMonth(another) && date.getDate() == another.getDate()

  def isBeforeOrOnToday = ((this - new Date(Date.now)) + (1 days)).valueOf < 0
  def isAfterOrOnToday = ((this - new Date(Date.now)) + (1 days)).valueOf > 0
}

object TimeUnit {
  implicit def int2TimeUnit(i: Int): TimeUnit = new TimeUnit(i)

  val MillisecondsInMinute = 60000
  val Day: Long = 86400000
  val Hour: Long = 3600000
  val Minute: Long = 60000
  val Second: Long = 1000
}

class TimeUnit(i: Int) {

  import TimeUnit._

  def days = new Date(i * Day)

  def hours = new Date(i * Hour)

  def minutes = new Date(i * Minute)

  def seconds = new Date(i * Second)
}