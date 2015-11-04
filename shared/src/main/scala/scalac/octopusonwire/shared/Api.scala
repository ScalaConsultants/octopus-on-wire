package scalac.octopusonwire.shared

import scalac.octopusonwire.shared.domain.{SimpleEvent, Event}

trait Api {
  def getFutureItems(limit: Int): Array[SimpleEvent]

  def getEventsForRange(from: Long, to: Long): Array[Event]

  def getEventAndUserJoined(eventId: Long): (Option[Event], Boolean)

  //TODO maybe we could get rid of the parentheses here
  def isUserLoggedIn(): Boolean

  def joinEvent(eventId: Long): Unit
}