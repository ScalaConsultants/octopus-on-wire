package scalac.octopusonwire.shared

import scalac.octopusonwire.shared.domain._

trait Api {
  def getFutureItems(limit: Int): Seq[SimpleEvent]

  def getEventsForRange(from: Long, to: Long): Seq[Event]

  def getUserEventInfo(eventId: EventId): Option[UserEventInfo]

  def getUserId(): Option[UserId]

  def isUserLoggedIn(): Boolean

  def joinEventAndGetJoins(eventId: EventId): Long
}