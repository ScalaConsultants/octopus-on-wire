package io.scalac.octopus.client.tools

import scala.scalajs.js.Date

object DateOps {
  implicit def date2DateOps(d: Date): DateOps = new DateOps(d)

  val MonthsShort = Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
  val Months = Array("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
  val Days = Array("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
}

class DateOps(date: Date) {
  def +(another: Date) =
    new Date(date.valueOf() + another.valueOf())

  def -(another: Date) =
    new Date(date.valueOf() - another.valueOf())

  def isSameYear(another: Date) =
    date.getFullYear() == another.getFullYear()

  def isSameMonth(another: Date) =
    isSameYear(another) && date.getMonth() == another.getMonth()

  def isSameDay(another: Date) =
    isSameMonth(another) && date.getDate() == another.getDate()
}

object TimeUnit {
  implicit def int2TimeUnit(i: Int): TimeUnit = new TimeUnit(i)

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