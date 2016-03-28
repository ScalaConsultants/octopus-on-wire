package domain

import com.google.inject.Inject
import config.DbConfig
import config.ServerConfig.MaxEventsInMonth
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}
import tools.{OffsetTime, TimeHelpers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
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

class EventDao @Inject()(dbConfig: DbConfig, eventJoins: EventJoinDao, eventFlags: EventFlagDao) {

  import dbConfig.db

  val eventQuery = TableQuery[Events]

  val eventById = (id: EventId) => eventQuery.filter(_.id === id)

  def countPastJoinsBy(id: UserId, currentTime: OffsetTime): Future[Int] = {
    val pastEvents = db.run {
      eventQuery.filterNot(_.endsAfter(currentTime)).map(_.id).result
    }

    val userJoins = eventJoins.eventJoinsByUserId(id)
    for {
      events <- pastEvents
      joins <- userJoins
    } yield events.count(ev => joins.map(_.eventId) contains ev)
  }

  def eventExists(eventId: EventId): Future[Boolean] =
    db.run {
      eventById(eventId).exists.result
    }

  def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Future[Seq[Event]] = {
    val serverOffset = TimeHelpers.readServerOffset()
    val eventsInPeriod = db.run {
      eventQuery.filter(_.isBetween(OffsetTime(from, serverOffset), OffsetTime(to, serverOffset))).result
    }

    val flagsByUser = userId.map(eventFlags.eventFlagsByUserId).getOrElse(Future.successful(Nil))

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

    val flagsByUser = userId.map(eventFlags.eventFlagsByUserId).getOrElse(Future.successful(Nil))

    for {
      events <- eventsInFuture
      flags <- flagsByUser
    } yield events.filterNot(ev => flags.map(_.eventId).contains(ev.id)).take(limit)
  }

  def addEventAndGetId(event: Event): Future[EventId] = db.run {
    eventQuery.returning(eventQuery.map(_.id)) += event
  }

  def findEventById(id: EventId)(implicit ec: ExecutionContext): Future[Event] = db.run {
    eventById(id).result.headOption
  } collect {
    case Some(ev) => ev
  }
}

