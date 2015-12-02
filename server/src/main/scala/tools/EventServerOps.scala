package tools

import java.util.Calendar

import scala.language.implicitConversions
import scalac.octopusonwire.shared.domain.Event

class EventServerOps(event: Event) {
  def isInTheFuture: Boolean = {
    val serverOffset = Calendar.getInstance.getTimeZone.getRawOffset

    val eventEndUTC = event.endDate - event.offset
    val currentUTC = System.currentTimeMillis - serverOffset

    eventEndUTC > currentUTC
  }
}

object EventServerOps{
  implicit def event2eventServerOps(event: Event): EventServerOps = new EventServerOps(event)
}