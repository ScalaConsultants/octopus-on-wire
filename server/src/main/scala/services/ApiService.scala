package services

import config.ServerConfig
import services.ApiService._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain._

trait EventSource {
  def getEvents: Seq[Event]

  def getEventsWhere(filter: Event => Boolean): Seq[Event]

  def joinEvent(userId: UserId, eventId: EventId): Unit

  def eventById(id: EventId): Option[Event]

  def countJoins(eventId: EventId): Long

  def getJoins(eventId: EventId): Set[UserId]

  def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean
}

object InMemoryEventSource extends EventSource {

  private val events: Array[Event] = Array(
    Event(EventId(1), "Warsaw Scala FortyFives - Scala Application Development #scala45pl", now + days(1), now + days(1) + hours(4), "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw", "http://www.meetup.com/WarszawScaLa/events/225320171/"),
    Event(EventId(2), "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", now + days(3) + hours(4), now + days(3) + hours(12), "Wydział MIMUW, Banacha 2, Warsaw", "http://www.meetup.com/Poland-CodiLime-Tech-Talk/events/226054818/"),
    Event(EventId(3), "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", now + days(6), now + days(6) + hours(8), "Javeo, Stadion Narodowy, Warsaw", "http://www.meetup.com/WarszawScaLa/events/226075320/"),
    Event(EventId(4), "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", now + days(8), now + days(8) + hours(8), "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226099814/"),
    Event(EventId(5), "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", now + days(10), now + days(10) + hours(8), "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/225901253/"),
    Event(EventId(6), "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup", now + days(14), now + days(14) + hours(8), "Fujitsu Rnd Łódź, Fabryczna 17, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226169070/"),
    Event(EventId(7), "TSUG (we are coming back after holiday break!)", now + days(18), now + days(18) + hours(8), "Olivia Business Centre, Olivia FOUR, aleja Grunwaldzka 472a, Gdansk", "http://www.meetup.com/Tricity-Scala-Users-Group/events/225945602/"),
    Event(EventId(8), "Best Scala event", now + days(28), now + days(28) + hours(8), "Some nice place", "https://scalac.io")
  )

  val eventJoins = TrieMap[EventId, Set[UserId]](
    EventId(1) -> Set(1136843, 1548278, 10749622, 192549, 13625545, 1097302, 82964, 345056, 390629, 4959786, 5664242).map(UserId(_)),
    EventId(2) -> Set(13625545, 1097302, 82964, 345056, 390629, 4959786).map(UserId(_))
  )

  override def getEvents: Seq[Event] = events

  override def getEventsWhere(filter: (Event) => Boolean): Seq[Event] = events.filter(filter)

  override def joinEvent(userId: UserId, eventId: EventId): Unit =
    eventById(eventId).foreach(event => {
      eventJoins(eventId) = eventJoins.getOrElse(eventId, Set.empty) + userId
    })

  override def eventById(id: EventId): Option[Event] = events find (_.id == id)

  override def countJoins(eventId: EventId): Long = eventJoins.getOrElse(eventId, Nil).size

  override def getJoins(eventId: EventId): Set[UserId] = eventJoins.getOrElse(eventId, Set.empty[UserId])

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean =
    eventJoins.get(event).exists(_.contains(userId))
}

class ApiService(userId: Option[UserId]) extends Api {

  val eventSource: EventSource = InMemoryEventSource

  override def getUserEventInfo(eventId: EventId): Option[UserEventInfo] =
    eventSource.eventById(eventId) match {
      case Some(event) =>
        Option(UserEventInfo(
          event,
          userId exists (token => eventSource.hasUserJoinedEvent(eventId, token)),
          eventSource.countJoins(eventId)
        ))
      case None => None
    }

  override def getUserInfo(): Option[UserInfo] =
    userId.flatMap { id =>
      Await.result(
        awaitable = UserCache.getOrFetchUserInfo(id),
        atMost = Duration.Inf
      )
    }

  override def getFutureItems(limit: Int): Seq[SimpleEvent] = {
    val now = System.currentTimeMillis()
    eventSource.getEventsWhere { event =>
      event.startDate >= now || event.endDate >= now
    } sortBy (_.startDate) take limit map (_.toSimple)
  }

  override def getEventsForRange(from: Long, to: Long): Seq[Event] =
    eventSource.getEventsWhere { event =>
      (event.startDate >= from && event.startDate <= to) ||
        (event.endDate >= from && event.endDate <= to)
    } take ServerConfig.MaxEventsInMonth

  override def isUserLoggedIn() = userId.isDefined

  override def joinEventAndGetJoins(eventId: EventId): Long = {
    userId match {
      case Some(id) =>
        eventSource.joinEvent(id, eventId)
      case None =>
        println("User not found")
    }
    eventSource.countJoins(eventId)
  }

  override def getUsersJoined(eventId: EventId, limit: Int): Set[UserInfo] =
    Await.result(
      awaitable =
        Future.sequence(
          eventSource.getJoins(eventId)
            .filterNot(userId.contains) take limit map UserCache.getOrFetchUserInfo
        ).map(_.flatten),
      atMost = Duration.Inf
    )

}

object ApiService {
  def hours(h: Int): Long = 3600000L * h

  def days(d: Int): Long = 3600000L * d * 24

  def now: Long = System.currentTimeMillis
}