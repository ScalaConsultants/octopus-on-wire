package domain

import config.DbConfig.db
import config.ServerConfig.MaxEventsInMonth
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain._

class Events(tag: Tag) extends Table[Event](tag, "events") {

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

  def isBetween(from: Long, to: Long) = (endDate - offset).between(from, to) || (startDate - offset).between(from, to)
}

object Events {
  val eventQuery = TableQuery[Events]

  val eventById = (id: EventId) => eventQuery.filter(_.id === id)

  def countPastJoinsBy(id: UserId, currentUTC: Long): Future[Int] = {
    val pastEvents = db.run{
      eventQuery.filterNot(_.endsAfter(currentUTC)).map(_.toSimpleTuple).result
    }.map(_.map(SimpleEvent.tupled))

    val userJoins = EventJoins.eventJoinsByUserId(id)
    for{
      events <- pastEvents
      joins <- userJoins
    } yield events.count(ev => joins.map(_.eventId).contains(ev.id))
  }

  def eventExists(eventId: EventId): Future[Boolean] =
    db.run {
      eventById(eventId).exists.result
    }

  def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Future[Seq[Event]] = {
    val uid = userId.getOrElse(NoUserId)

    val eventsInPeriod = db.run {
      eventQuery.filter(_.isBetween(from, to)).map(_.toTuple).result
    }.map(_.map((Event.apply _).tupled))

    val flagsByUser = EventFlags.eventFlagsByUserId(uid)

    for {
      events <- eventsInPeriod
      flags <- flagsByUser
    } yield events.filterNot(ev => flags.exists(_.eventId == ev.id)).take(MaxEventsInMonth)
  }

  def getFutureUnflaggedEvents(userId: Option[UserId], limit: Int, now: Long): Future[Seq[SimpleEvent]] = {
    val uid = userId.getOrElse(UserId(-1))

    val eventsInFuture = db.run {
      eventQuery
        .filter(_.endsAfter(now))
        .map(_.toSimpleTuple).result
    }.map(_.map(SimpleEvent.tupled))

    val flagsByUser = EventFlags.eventFlagsByUserId(uid)

    for {
      events <- eventsInFuture
      flags <- flagsByUser
    } yield events.filterNot(ev => flags.map(_.eventId).contains(ev.id)).take(limit)
  }

  def addEventAndGetId(event: Event): Future[EventId] = db.run {
    eventQuery.returning(eventQuery.map(_.id)) += event
  }

  def findEventById(id: EventId): Future[Option[Event]] = db.run {
    eventById(id).map(_.toTuple).result.headOption
  }.map(_.map((Event.apply _).tupled))
}

