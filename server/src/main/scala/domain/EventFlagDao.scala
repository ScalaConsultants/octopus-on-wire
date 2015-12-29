package domain

import config.DbConfig._
import domain.Mappers._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{EventFlag, EventId, UserId}
import scalac.octopusonwire.shared.domain._

class EventFlagDao(tag: Tag) extends Table[EventFlag](tag, "event_flags") {
  def eventId = column[EventId]("event_id")(EventIdMapper)

  def userId = column[UserId]("user_id")(UserIdMapper)

  override def * : ProvenShape[EventFlag] = toTuple <>(EventFlag.tupled, EventFlag.unapply)

  def toTuple = (eventId, userId)
}

object EventFlagDao {

  val eventFlags = TableQuery[EventFlagDao]

  val eventFlagsById = (id: EventId) => eventFlags.filter(_.eventId === id)

  val eventFlagsByUserId = (id: UserId) => db.run {
    eventFlags.filter(_.userId === id).map(_.toTuple).result
  }.map(_.map(EventFlag.tupled))

  def getFlaggers(eventId: EventId): Future[Set[UserId]] = db.run {
    eventFlagsById(eventId).map(_.userId).result
  }.map(_.toSet)

  def countFlags(eventId: EventId): Future[Int] = db.run {
    eventFlags.filter(ef => ef.eventId === eventId).length.result
  }

  def userHasFlaggedEvent(eventId: EventId, by: UserId): Future[Boolean] =
    db.run {
      eventFlagsById(eventId).filter(_.userId === by).exists.result
    }

  def flagEvent(eventId: EventId, by: UserId): Future[Boolean] = EventDao.eventExists(eventId).flatMap {
    case true =>
      db.run {
        eventFlags.returning(eventFlags.map(_.eventId)) += EventFlag(eventId, by)
      }.map(_ == eventId)
    case _ => Future.successful(false)
  }
}