package scalac.octopusonwire.shared

package object domain {
  case class EventFlag(eventId: EventId, userId: UserId)
  case class EventJoin(eventId: EventId, userId: UserId)
}