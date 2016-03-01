package scalac.octopusonwire.shared.domain

import scala.language.postfixOps
import scalac.octopusonwire.shared.config.SharedConfig.MillisecondsInHour
import scalac.octopusonwire.shared.domain.Event._
import scalac.octopusonwire.shared.tools.LongRangeOps._

//Event sans validation
class BaseEvent(val id: EventId, val name: String, val startDate: Long, val endDate: Long, val offset: Long, val location: String, val url: String) {
  def toSimple: SimpleEvent = SimpleEvent(id, name)
}

case class Event(override val id: EventId, override val name: String,
                 override val startDate: Long, override val endDate: Long, override val offset: Long,
                 override val location: String, override val url: String)
  extends BaseEvent(id, name, startDate, endDate, offset, location, url) {

  val invalidFields = invalidFieldsIn(this)
  require(invalidFields.isEmpty, invalidFields mkString)
}

object Event {
  def from(base: BaseEvent) = Event(base.id, base.name, base.startDate, base.endDate, base.offset, base.location, base.url)

  val InvalidNameMessage = "The name should be between 3 and 100 characters in length"
  val InvalidDatesMessage = "The start date must be before end date"
  val InvalidOffsetMessage = "Invalid timezone supplied"
  val InvalidLocationMessage = "The location should be between 3 and 100 characters in length"
  val InvalidURLMessage = "The URL should be between 3 and 100 characters in length"

  def invalidFieldsIn(event: BaseEvent): Set[String] = Map(
    InvalidNameMessage -> (event.name.length inRange(3, 100)),
    InvalidDatesMessage -> (event.startDate < event.endDate),
    InvalidOffsetMessage -> (event.offset inRange(-15 * MillisecondsInHour, 15 * MillisecondsInHour)),
    InvalidLocationMessage -> (event.location.length inRange(3, 100)),
    InvalidURLMessage -> (event.url.length inRange(3, 100))
  ).filter(_._2 == false).keySet
}

case class EventId(value: Long) {
  override def toString: String = this == NoId match {
    case true => "NoId"
    case _ => s"EventId($value)"
  }
}

object NoId extends EventId(-1)

case class SimpleEvent(id: EventId, name: String)

case class UserEventInfo(event: Event, userJoined: Boolean, joinCount: Long, eventActive: Boolean)