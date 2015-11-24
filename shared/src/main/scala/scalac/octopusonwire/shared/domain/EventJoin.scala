package scalac.octopusonwire.shared.domain

case class EventJoin(joinCount: Long, message: EventJoinMessage)
case class EventJoinMessage(details: String)

class EventJoinMessageBuilder{
  def apply = EventJoinMessage(toString)
  def unapply(ejm: EventJoinMessage) = if(ejm.details == toString) Some(ejm.details) else None
}
object EventJoinMessageBuilder{
  case object `Joined` extends EventJoinMessageBuilder
  case object `Event not found` extends EventJoinMessageBuilder
  case object `User not found` extends EventJoinMessageBuilder
  case object `Trying to join past event` extends EventJoinMessageBuilder
}