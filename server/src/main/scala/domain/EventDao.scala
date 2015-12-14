package domain

import config.DbConfig
import domain.EventDao.EventIdMapper
import domain.UserDao.UserIdMapper
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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

class EventFlagDao(tag: Tag) extends Table[EventFlag](tag, "event_flags") {
  def eventId = column[EventId]("event_id")(EventIdMapper)

  def userId = column[UserId]("user_id")(UserIdMapper)

  override def * : ProvenShape[EventFlag] = toTuple <>(EventFlag.tupled, EventFlag.unapply)

  def toTuple = (eventId, userId)
}

object EventDao {
  implicit val EventIdMapper = MappedColumnType.base[EventId, Long](_.value, EventId.apply)
  implicit val UserIdMapper = UserDao.UserIdMapper
  val db = DbConfig.db

  val events = TableQuery[EventDao]

  //  val users = TableQuery[UserDao]
  val eventFlags = TableQuery[EventFlagDao]

  def getFutureUnflaggedEvents(userId: Option[UserId], limit: Int, now: Long): Future[Seq[SimpleEvent]] = {
    val uid = userId.getOrElse(UserId(-1))
    db.run {
      events
        .filter(_.endsAfter(now))
        .map(_.toSimpleTuple).result
    }.flatMap(futureEvents => {
      val uid = userId.getOrElse(UserId(-1))

      db.run {
        eventFlags.filter(_.userId === uid).map(_.eventId).result
      }.map { flaggedEvents =>
        futureEvents
          .filterNot { case (eventId, _) => flaggedEvents.contains(eventId) }
          .take(limit)
          .map(SimpleEvent.tupled)
      }
    })
  }

  def addEventAndGetId(event: Event): Future[EventId] = DbConfig.db.run {
    events.returning(events.map(_.id)) += event
  }

  def findEventById(id: EventId): Future[Option[Event]] = db.run {
    events.filter(event => event.id === id).map(_.toTuple).result.headOption
  }.map(_.map((Event.apply _).tupled))
}

