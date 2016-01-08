package domain

import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.JoinSuccessful
import scalac.octopusonwire.shared.domain.{EventId, EventJoin, UserId, _}

class EventJoins(tag: Tag) extends EventUserAbstractDao[EventJoin](tag, "event_joins") {
  override def * : ProvenShape[EventJoin] = toTuple <>(EventJoin.tupled, EventJoin.unapply)
}

object EventJoins extends EventUserAbstractDaoCompanion[EventJoin, EventJoins] {

  val db: JdbcBackend#DatabaseDef = DbConfig.db

  val allQuery = TableQuery[EventJoins]

  val eventJoinsById = queryByEventId _

  def eventJoinsByUserId(id: UserId) = getByUserId(id)

  def getJoiners(eventId: EventId): Future[Set[UserId]] = db.run {
    eventJoinsById(eventId).map(_.userId).result
  }.map(_.toSet)

  val countJoins = getCountByEventId _

  val userHasJoinedEvent = getExistsByEventIdAndUserId _

  def joinEvent(eventId: EventId, by: UserId): Future[EventJoinMessage] =
    db.run {
      allQuery += EventJoin(eventId, by)
    }.map(_ => JoinSuccessful.apply)
}