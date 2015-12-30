package scalac.octopusonwire.shared

package object domain {
  trait EventUserPair{
    val eventId: EventId
    val userId: UserId
  }
  case class EventFlag(eventId: EventId, userId: UserId) extends EventUserPair
  case class EventJoin(eventId: EventId, userId: UserId) extends EventUserPair
}