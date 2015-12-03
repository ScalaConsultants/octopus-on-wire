package scalac.octopusonwire.shared

import scalac.octopusonwire.shared.domain._

trait Api {
  def getFutureItems(limit: Int): Seq[SimpleEvent]

  def getEventsForRange(from: Long, to: Long): Seq[Event]

  def getUserEventInfo(eventId: EventId): Option[UserEventInfo]

  def getUserInfo(): Option[UserInfo]

  def isUserLoggedIn(): Boolean

  def joinEventAndGetJoins(eventId: EventId): EventJoin

  def getUsersJoined(eventId: EventId, limit: Int): Set[UserInfo]

  def addEvent(event: Event): EventAddition

  def flagEvent(eventId: EventId): Boolean

  def getUserReputation(): Option[UserReputationInfo]
}