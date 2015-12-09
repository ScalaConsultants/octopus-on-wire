package domain

import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scalac.octopusonwire.shared.domain.{Event, EventId}

class Events(tag: Tag) extends Table[Event](tag, "events") {

  def id = column[EventId]("id", O.PrimaryKey, O.AutoInc)(EventIdMapper)

  def name = column[String]("name")

  def startDate = column[Long]("start_date")

  def endDate = column[Long]("end_date")

  def offset = column[Long]("offset")

  def location = column[String]("location")

  def url = column[String]("url")

  implicit val EventIdMapper = MappedColumnType.base[EventId, Long](_.value, EventId.apply)

  override def * : ProvenShape[Event] = (id, name, startDate, endDate, offset, location, url) <>((Event.apply _).tupled, Event.unapply)

}

object Events {
  val events = TableQuery[Events]
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val db = dbConfig.db

  implicit val EventIdMapper = MappedColumnType.base[EventId, Long](_.value, EventId.apply)

  val getEvents = db.run {
    events.map(p => (p.id, p.name, p.startDate, p.endDate, p.offset, p.location, p.url)).result
  }.map(_.map((Event.apply _).tupled))
}