package domain

import config.DbConfig._
import domain.Mappers._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{JoinSuccessful, AlreadyJoined, EventNotFound}
import scalac.octopusonwire.shared.domain.{EventJoin, EventId, UserId}
import scalac.octopusonwire.shared.domain._

class EventJoinDao(tag: Tag) extends Table[EventJoin](tag, "event_joins") {
  def eventId = column[EventId]("event_id")(EventIdMapper)

  def userId = column[UserId]("user_id")(UserIdMapper)

  override def * : ProvenShape[EventJoin] = toTuple <>(EventJoin.tupled, EventJoin.unapply)

  def toTuple = (eventId, userId)
}

object EventJoinDao {

  val eventJoins = TableQuery[EventJoinDao]

  val eventJoinsById = (id: EventId) => eventJoins.filter(_.eventId === id)

  val eventJoinsByUserId: (UserId) => Future[Seq[EventJoin]] = id =>
    db.run {
      eventJoins.filter(_.userId === id).map(_.toTuple).result
    }.map(_.map(EventJoin.tupled))

  def getJoiners(eventId: EventId): Future[Set[UserId]] = db.run {
    eventJoinsById(eventId).map(_.userId).result
  }.map(_.toSet)

  def countJoins(eventId: EventId): Future[Long] =
    db.run {
      eventJoins.filter(ef => ef.eventId === eventId).length.result
    }.map(_.toLong)

  def userHasJoinedEvent(eventId: EventId, by: UserId): Future[Boolean] =
    db.run {
      eventJoinsById(eventId).filter(_.userId === by).exists.result
    }

  def joinEvent(eventId: EventId, by: UserId): Future[EventJoinMessage] =
    userHasJoinedEvent(eventId, by).flatMap {
      case false =>
        db.run {
          eventJoins.returning(eventJoins.map(_.eventId)) += EventJoin(eventId, by)
        }.filter(_ == eventId).map(_ => JoinSuccessful)
      case _ => Future.successful(AlreadyJoined)
    }.map(_.apply)
}