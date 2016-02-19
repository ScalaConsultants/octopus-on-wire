package tools

import scala.language.implicitConversions
import scalac.octopusonwire.shared.domain.Event

class EventServerOps(event: Event) {
  def isInTheFuture: Boolean = {
    val eventEndUTC = event.endDate - event.offset

    eventEndUTC > TimeHelpers.currentUTC
  }
}

object EventServerOps{
  implicit def event2eventServerOps(event: Event): EventServerOps = new EventServerOps(event)
}