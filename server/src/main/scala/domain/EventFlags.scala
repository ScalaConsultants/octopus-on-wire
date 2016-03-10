package domain

import com.google.inject.Inject
import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{EventFlag, EventId, UserId}

class EventFlags(tag: Tag) extends EventUserAbstractDao[EventFlag](tag, "event_flags") {
  override def * : ProvenShape[EventFlag] = toTuple <>(EventFlag.tupled, EventFlag.unapply)
}

class EventFlagDao @Inject() (dbConfig: DbConfig) extends EventUserAbstractDaoCompanion[EventFlag, EventFlags] {
  override val db = dbConfig.db

  def allQuery = TableQuery[EventFlags]

  val getFlaggers = getAllUserIdByEventId _

  val userHasFlaggedEvent = getExistsByEventIdAndUserId _

  def eventFlagsByUserId(idOpt: Option[UserId]): Future[Seq[EventFlag]] =
    idOpt.map(getByUserId).getOrElse(Future.successful(Nil))

  def flagEvent(eventId: EventId, by: UserId): Future[Boolean] =
    userHasFlaggedEvent(eventId, by).flatMap {
      case false =>
        db.run {
          allQuery.returning(allQuery.map(_.eventId)) += EventFlag(eventId, by)
        }.map(_ == eventId)
      case _ => Future.successful(false)
    }
}