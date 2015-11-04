package services

import config.ServerConfig
import services.ApiService._
import services.EventSource._

import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.{Event, SimpleEvent}

object EventSource {
  val events: Array[Event] = Array(
    Event(1, "Warsaw Scala FortyFives - Scala Application Development #scala45pl", now + days(1), now + days(1) + hours(4), "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw", "http://www.meetup.com/WarszawScaLa/events/225320171/"),
    Event(2, "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", now + days(3) + hours(4), now + days(3) + hours(12), "Wydział MIMUW, Banacha 2, Warsaw", "http://www.meetup.com/Poland-CodiLime-Tech-Talk/events/226054818/"),
    Event(3, "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", now + days(6), now + days(6) + hours(8), "Javeo, Stadion Narodowy, Warsaw", "http://www.meetup.com/WarszawScaLa/events/226075320/"),
    Event(4, "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", now + days(8), now + days(8) + hours(8), "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226099814/"),
    Event(5, "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", now + days(10), now + days(10) + hours(8), "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/225901253/"),
    Event(6, "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup", now + days(14), now + days(14) + hours(8), "Fujitsu Rnd Łódź, Fabryczna 17, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226169070/"),
    Event(7, "TSUG (we are coming back after holiday break!)", now + days(18), now + days(18) + hours(8), "Olivia Business Centre, Olivia FOUR, aleja Grunwaldzka 472a, Gdansk", "http://www.meetup.com/Tricity-Scala-Users-Group/events/225945602/"),
    Event(8, "Best Scala event", now + days(28), now + days(28) + hours(8), "Some nice place", "https://scalac.io")
  )

  //todo don't keep tokens here
  var eventJoins = List[(String, Long)]()

  def joinEvent(userToken: Option[String], eventId: Long): Unit = userToken match {
    case Some(token) =>
      if (eventById(eventId).isDefined)
        eventJoins = (token, eventId) :: eventJoins
    case None =>
  }

  def eventById(id: Long): Option[Event] = events find (_.id == id)
}

class ApiService(userToken: Option[String]) extends Api {

  override def getEventAndUserJoined(eventId: Long): (Option[Event], Boolean) =
    eventById(eventId) match {
      case Some(event) =>
        (Option(event),
          userToken match {
            case Some(token) => eventJoins contains ((token, event.id))
            case None => false
          })
      case None => (None, false)
    }

  override def getFutureItems(limit: Int): Array[SimpleEvent] = {
    val now = System.currentTimeMillis()
    events.filter { event =>
      event.startDate > now || event.endDate > now
    } sortBy (_.startDate) take limit map(_.toSimple)
  }

  override def getEventsForRange(from: Long, to: Long): Array[Event] =
    events.filter { event =>
      (event.startDate >= from && event.startDate <= to) ||
        (event.endDate >= from && event.endDate <= to)
    } take ServerConfig.MaxEventsInMonth

  override def isUserLoggedIn() = userToken.isDefined

  override def joinEvent(eventId: Long): Unit = EventSource.joinEvent(userToken, eventId)
}

object ApiService {
  def hours(h: Int): Long = 3600000L * h

  def days(d: Int): Long = 3600000L * d * 24

  def now: Long = System.currentTimeMillis
}