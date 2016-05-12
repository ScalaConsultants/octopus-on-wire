package scalac.octopusonwire.shared.domain

case class EventJoinInfo(joinCount: Long, message: EventJoinMessage)

case class EventJoinMessage(details: String)

class EventJoinMessageBuilder {
  def apply = EventJoinMessage(toString)

  def unapply(ejm: EventJoinMessage) = if (ejm.details == toString) Some(ejm.details) else None
}

object EventJoinMessageBuilder {

  case object JoinSuccessful extends EventJoinMessageBuilder

  case object AlreadyJoined extends EventJoinMessageBuilder

  case object EventNotFound extends EventJoinMessageBuilder

  case object UserNotFound extends EventJoinMessageBuilder

  case object TryingToJoinPastEvent extends EventJoinMessageBuilder

  val allStates = JoinSuccessful :: AlreadyJoined :: EventNotFound :: UserNotFound :: TryingToJoinPastEvent :: Nil
}