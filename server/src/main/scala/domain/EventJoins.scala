package domain

import com.google.inject.Inject
import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{AlreadyJoined, JoinSuccessful}
import scalac.octopusonwire.shared.domain.{EventId, EventJoin, UserId, _}

class EventJoins(tag: Tag) extends EventUserAbstractDao[EventJoin](tag, "event_joins") {
  override def * : ProvenShape[EventJoin] = toTuple <>(EventJoin.tupled, EventJoin.unapply)
}

class EventJoinDao @Inject()(dbConfig: DbConfig) extends EventUserAbstractDaoCompanion[EventJoin, EventJoins] {
  override val db = dbConfig.db

  val allQuery = TableQuery[EventJoins]

  def eventJoinsByUserId(id: UserId) = getByUserId(id)

  def getJoiners(eventId: EventId): Future[Set[UserId]] = db.run {
    queryByEventId(eventId).map(_.userId).result
  }.map(_.toSet)

  val countJoins = getCountByEventId _

  val userHasJoinedEvent = getExistsByEventIdAndUserId _

  def joinEvent(eventId: EventId, by: UserId): Future[EventJoinMessage] =
    userHasJoinedEvent(eventId, by).flatMap {
      case false =>
        db.run {
          allQuery += EventJoin(eventId, by)
        }.map(_ => JoinSuccessful.apply)
      case _ => Future.successful(AlreadyJoined.apply)
    }
}