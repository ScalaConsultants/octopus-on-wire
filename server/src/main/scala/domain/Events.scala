package domain

import config.DbConfig.db
import config.ServerConfig.MaxEventsInMonth
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}
import tools.{OffsetTime, TimeHelpers}

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

  def endsAfter(time: OffsetTime) = (endDate - offset) > time.value

  def isBetween(from: OffsetTime, to: OffsetTime) =
    (endDate - offset).between(from.value, to.value) || (startDate - offset).between(from.value, to.value)
}

object Events {
  val eventQuery = TableQuery[Events]

  val eventById = (id: EventId) => eventQuery.filter(_.id === id)

  def countPastJoinsBy(id: UserId, currentTime: OffsetTime): Future[Int] = {
    val pastEvents = db.run {
      eventQuery.filterNot(_.endsAfter(currentTime)).map(_.toSimpleTuple).result
    }.map(_.map(SimpleEvent.tupled))

    val userJoins = EventJoins.eventJoinsByUserId(id)
    for {
      events <- pastEvents
      joins <- userJoins
    } yield events.count(ev => joins.map(_.eventId).contains(ev.id))
  }

  def eventExists(eventId: EventId): Future[Boolean] =
    db.run {
      eventById(eventId).exists.result
    }

  def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Future[Seq[Event]] = {
    val serverOffset = TimeHelpers.getServerOffset
    val eventsInPeriod = db.run {
      eventQuery.filter(_.isBetween(OffsetTime(from, serverOffset), OffsetTime(to,serverOffset))).result
    }

    val flagsByUser = EventFlags.eventFlagsByUserId(userId)

    for {
      events <- eventsInPeriod
      flags <- flagsByUser
    } yield events.filterNot(ev => flags.exists(_.eventId == ev.id)).take(MaxEventsInMonth)
  }

  def getFutureUnflaggedEvents(userId: Option[UserId], limit: Int, now: OffsetTime): Future[Seq[SimpleEvent]] = {
    val eventsInFuture = db.run {
      eventQuery
        .filter(_.endsAfter(now))
        .sortBy(_.endDate)
        .map(_.toSimpleTuple).result
    }.map(_.map(SimpleEvent.tupled))

    val flagsByUser = EventFlags.eventFlagsByUserId(userId)

    for {
      events <- eventsInFuture
      flags <- flagsByUser
    } yield events.filterNot(ev => flags.map(_.eventId).contains(ev.id)).take(limit)
  }

  def addEventAndGetId(event: Event): Future[EventId] = db.run {
    eventQuery.returning(eventQuery.map(_.id)) += event
  }

  def findEventById(id: EventId): Future[Option[Event]] = db.run {
    eventById(id).result.headOption
  }
}

