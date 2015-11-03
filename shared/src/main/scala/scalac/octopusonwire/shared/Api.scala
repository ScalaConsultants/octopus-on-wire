package scalac.octopusonwire.shared

import scalac.octopusonwire.shared.domain.Event

trait Api {
  def getFutureItems(limit: Int): Array[Event]

  def getEventAndUserJoined(eventId: Long): (Option[Event], Boolean)

  //TODO maybe we could get rid of the parentheses here
  def isUserLoggedIn(): Boolean

  def joinEvent(eventId: Long): Unit
}