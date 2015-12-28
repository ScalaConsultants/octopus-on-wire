package domain

import scalac.octopusonwire.shared.domain.{EventId, UserId}
import slick.driver.PostgresDriver.api._

/**
  * Contains mappers for "not really" value classes stored as ID fields in the DB.
  **/
object Mappers {
  implicit val EventIdMapper = MappedColumnType.base[EventId, Long](_.value, EventId.apply)
  implicit val UserIdMapper = MappedColumnType.base[UserId, Long](_.value, UserId.apply)
}