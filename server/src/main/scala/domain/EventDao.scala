package domain

import config.DbConfig.db
import domain.Mappers._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain._

class EventDao(tag: Tag) extends Table[Event](tag, "events") {

  def id = column[EventId]("id", O.PrimaryKey, O.AutoInc)(EventIdMapper)

  def name = column[String]("name")

  def startDate = column[Long]("start_date")

  def endDate = column[Long]("end_date")

  def offset = column[Long]("offset")

  def location = column[String]("location")

  def url = column[String]("url")

  override def * : ProvenShape[Event] = toTuple <>((Event.apply _).tupled, Event.unapply)

  def toTuple = (id, name, startDate, endDate, offset, location, url)

  def toSimpleTuple = (id, name)

  def endsAfter(time: Long) = (endDate - offset) > time
}

object EventDao {

  val events = TableQuery[EventDao]

  val eventById = (id: EventId) => events.filter(_.id === id)

  def eventExists(eventId: EventId): Future[Boolean] =
    db.run {
      eventById(eventId).exists.result
    }

  def getFutureUnflaggedEvents(userId: Option[UserId], limit: Int, now: Long): Future[Seq[SimpleEvent]] = {
    db.run {
      events
        .filter(_.endsAfter(now))
        .map(_.toSimpleTuple).result
    }.flatMap(futureEvents => {
      val uid = userId.getOrElse(UserId(-1))

      db.run {
        EventFlagDao.eventFlags.filter(_.userId === uid).map(_.eventId).result
      }.map { flaggedEvents =>
        futureEvents
          .filterNot { case (eventId, _) => flaggedEvents.contains(eventId) }
          .take(limit)
          .map(SimpleEvent.tupled)
      }
    })
  }

  def addEventAndGetId(event: Event): Future[EventId] = db.run {
    events.returning(events.map(_.id)) += event
  }

  def findEventById(id: EventId): Future[Option[Event]] = db.run {
    eventById(id).map(_.toTuple).result.headOption
  }.map(_.map((Event.apply _).tupled))
}

