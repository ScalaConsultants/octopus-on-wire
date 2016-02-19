package tools

import java.util.Calendar

object TimeHelpers {
  def hours(h: Int): Long = 3600000L * h

  def days(d: Int): Long = 3600000L * d * 24

  def now: Long = System.currentTimeMillis

  def currentUTC = System.currentTimeMillis - getServerOffset

  def getServerOffset = Calendar.getInstance.getTimeZone.getRawOffset
}

class OffsetTime private(val value: Long) extends AnyVal

object OffsetTime {
  def serverCurrent = apply(TimeHelpers.currentUTC, TimeHelpers.getServerOffset)
  def apply(time: Long, offset: Long) = new OffsetTime(time - offset)
}