import scalac.octopusonwire.shared.domain.{UserId, EventId}
import slick.driver.PostgresDriver.api._

package object domain {
  implicit val EventIdMapper = MappedColumnType.base[EventId, Long](_.value, EventId.apply)
  implicit val UserIdMapper = MappedColumnType.base[UserId, Long](_.value, UserId.apply)
}
