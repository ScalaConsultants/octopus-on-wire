package scalac.octopusonwire.shared.domain

trait EventUserPair {
  val eventId: EventId
  val userId: UserId
}

case class EventFlag(eventId: EventId, userId: UserId) extends EventUserPair

case class EventJoin(eventId: EventId, userId: UserId) extends EventUserPair

case class TokenPair(token: String, userId: UserId)

case class UserFriendPair(userId: UserId, friendId: UserId)