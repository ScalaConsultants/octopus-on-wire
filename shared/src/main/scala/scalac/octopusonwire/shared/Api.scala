package scalac.octopusonwire.shared

import scala.concurrent.Future
import scalac.octopusonwire.shared.domain._

trait Api {
  def getFutureItems(limit: Int): Future[Seq[SimpleEvent]]

  def getEventsForRange(from: Long, to: Long): Future[Seq[Event]]

  def getUserEventInfo(eventId: EventId): Future[Option[UserEventInfo]]

  def getUserInfo(): Future[Option[UserInfo]]

  def joinEventAndGetJoins(eventId: EventId): Future[EventJoinInfo]

  def getUsersJoined(eventId: EventId, limit: Int): Future[Set[UserInfo]]

  def addEvent(event: Event): Future[EventAddition]

  def flagEvent(eventId: EventId): Future[Boolean]

  def getUserReputation(): Future[Option[UserReputationInfo]]
}