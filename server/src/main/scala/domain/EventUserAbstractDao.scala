package domain

import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend
import slick.lifted.{TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scalac.octopusonwire.shared.domain.{EventId, UserId, _}

abstract class EventUserAbstractDao[T <: EventUserPair](tag: Tag, tableName: String) extends Table[T](tag, tableName) {
  def eventId = column[EventId]("event_id")(EventIdMapper)

  def userId = column[UserId]("user_id")(UserIdMapper)

  def toTuple = (eventId, userId)
}

trait EventUserAbstractDaoCompanion[Entity <: EventUserPair, Dao <: EventUserAbstractDao[Entity]] {

  def db: JdbcBackend#DatabaseDef

  def allQuery: TableQuery[Dao]

  def queryByEventId(id: EventId) = allQuery.filter(_.eventId === id)

  def getByUserId(id: UserId) = db.run {
    allQuery.filter(_.userId === id).result
  }

  def getAllUserIdByEventId(id: EventId) = db.run {
    queryByEventId(id).map(_.userId).result
  }.map(_.toSet)

  def getCountByEventId(id: EventId) = db.run {
    queryByEventId(id).length.result
  }

  def getExistsByEventIdAndUserId(eventId: EventId, userId: UserId) = db.run {
    queryByEventId(eventId).filter(_.userId === userId).exists.result
  }
}
