package domain

import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{EventFlag, EventId, UserId, _}

class EventFlags(tag: Tag) extends EventUserAbstractDao[EventFlag](tag, "event_flags") {
  override def * : ProvenShape[EventFlag] = toTuple <>(EventFlag.tupled, EventFlag.unapply)
}

object EventFlags extends EventUserAbstractDaoCompanion[EventFlag, EventFlags] {
  val db = DbConfig.db

  def allQuery = TableQuery[EventFlags]

  val getFlaggers = getAllUserIdByEventId _

  val countFlags = getCountByEventId _

  val userHasFlaggedEvent = getExistsByEventIdAndUserId _

  def eventFlagsByUserId(id: UserId) = getByUserId(id).map(_.map(EventFlag.tupled))

  def flagEvent(eventId: EventId, by: UserId): Future[Boolean] = Events.eventExists(eventId).flatMap {
    case true =>
      db.run {
        allQuery.returning(allQuery.map(_.eventId)) += EventFlag(eventId, by)
      }.map(_ == eventId)
    case _ => Future.successful(false)
  }
}