package tools

object TimeHelpers {
  def hours(h: Int): Long = 3600000L * h

  def days(d: Int): Long = 3600000L * d * 24

  def now: Long = System.currentTimeMillis
}
