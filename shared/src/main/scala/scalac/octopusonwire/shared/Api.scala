package scalac.octopusonwire.shared

import scalac.octopusonwire.shared.domain.{UserEventInfo, SimpleEvent, Event}

trait Api {
  def getFutureItems(limit: Int): Array[SimpleEvent]

  def getEventsForRange(from: Long, to: Long): Array[Event]

  def getUserEventInfo(eventId: Long): Option[UserEventInfo]

  //TODO maybe we could get rid of the parentheses here
  def isUserLoggedIn(): Boolean

  def joinEventAndGetJoins(eventId: Long): Long
}