package data

import config.ServerConfig
import tools.EventServerOps._
import tools.TimeHelpers._

import scala.collection.concurrent.TrieMap
import scala.language.postfixOps
import scalac.octopusonwire.shared.domain.EventJoinMessageBuilder.{AlreadyJoined, EventNotFound, JoinSuccessful}
import scalac.octopusonwire.shared.domain._
import scalac.octopusonwire.shared.tools.LongRangeOps._

object InMemoryEventSource extends InMemoryEventSource

class InMemoryEventSource extends EventSource {

  private var events: List[Event] = List(
    Event(EventId(1), "Warsaw Scala FortyFives - Scala Application Development #scala45pl", now + days(1), now + days(1) + hours(4), 3600000, "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw", "http://www.meetup.com/WarszawScaLa/events/225320171/"),
    Event(EventId(2), "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", now + days(3) + hours(4), now + days(3) + hours(12), 3600000, "Wydział MIMUW, Banacha 2, Warsaw", "http://www.meetup.com/Poland-CodiLime-Tech-Talk/events/226054818/"),
    Event(EventId(3), "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", now + days(6), now + days(6) + hours(8), 3600000, "Javeo, Stadion Narodowy, Warsaw", "http://www.meetup.com/WarszawScaLa/events/226075320/"),
    Event(EventId(4), "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", now + days(8), now + days(8) + hours(8), 3600000, "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226099814/"),
    Event(EventId(5), "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", now + days(10), now + days(10) + hours(8), 3600000, "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/225901253/"),
    Event(EventId(6), "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup", now + days(14), now + days(14) + hours(8), 3600000, "Fujitsu Rnd Łódź, Fabryczna 17, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226169070/"),
    Event(EventId(7), "TSUG (we are coming back after holiday break!)", now + days(18), now + days(18) + hours(8), 3600000, "Olivia Business Centre, Olivia FOUR, aleja Grunwaldzka 472a, Gdansk", "http://www.meetup.com/Tricity-Scala-Users-Group/events/225945602/"),
    Event(EventId(8), "Best Scala event", now + days(28), now + days(28) + hours(8), 3600000, "Some nice place", "https://scalac.io")
  )

  val eventJoins = TrieMap[EventId, Set[UserId]](
    EventId(1) -> Set(1136843, 1548278, 10749622, 192549, 13625545, 1097302, 82964, 345056, 390629, 4959786, 5664242).map(UserId(_)),
    EventId(2) -> Set(13625545, 1097302, 82964, 345056, 390629, 4959786).map(UserId(_))
  )

  var addedJoins = false

  override def getSimpleFutureEventsNotFlaggedByUser(userId: Option[UserId], limit: Int): Seq[SimpleEvent] = {
    getEvents.filter { event =>
      !userId.exists(hasUserFlagged(_, event.id)) && event.isInTheFuture
    }.take(limit).map(_.toSimple)
  }

  override def getEventsBetweenDatesNotFlaggedBy(from: Long, to: Long, userId: Option[UserId]): Seq[Event] =
    getEvents.filter { event =>
      !userId.exists(hasUserFlagged(_, event.id)) &&
        (event.startDate.inRange(from, to) || event.endDate.inRange(from, to))
    }.take(ServerConfig.MaxEventsInMonth)

  private def hasUserFlagged(userId: UserId, eventId: EventId) =
    getFlaggers(eventId) contains userId

  def addFakeUserJoins(userId: UserId) =
    if (!addedJoins) {
      val oldEventCount = events.length
      1 to ServerConfig.PastJoinsRequiredToAddEvents foreach { i =>
        events ::= Event(EventId(i + oldEventCount), s"Some event $i", now - days(2), now - days(1), 3600000, "The cloud", "http://example.com")
        eventJoins(EventId(i + oldEventCount)) = Set(userId)
      }
      addedJoins = true
    }

  private val flags = TrieMap[EventId, Set[UserId]]()

  override def getFlaggers(eventId: EventId): Set[UserId] = flags.getOrElse(eventId, Set.empty)

  override def countPastJoinsBy(id: UserId): Long =
    getPastEvents.map { event =>
      getJoins(event.id)
        .find(_ == id).size
    }.sum

  private def getEvents: Seq[Event] = events

  private def getPastEvents: Seq[Event] = getEvents.filterNot(_ isInTheFuture)

  override def joinEvent(userId: UserId, eventId: EventId): EventJoinMessage = {
    if (hasUserJoinedEvent(eventId, userId)) AlreadyJoined
    else {
      eventJoins(eventId) = eventJoins.getOrElse(eventId, Set.empty) + userId
      JoinSuccessful
    }
  }.apply

  override def eventById(id: EventId): Option[Event] = getEvents find (_.id == id)

  override def countJoins(eventId: EventId): Long = eventJoins.getOrElse(eventId, Nil).size

  override def getJoins(eventId: EventId): Set[UserId] = eventJoins.getOrElse(eventId, Set.empty[UserId])

  override def hasUserJoinedEvent(event: EventId, userId: UserId): Boolean =
    eventJoins.get(event).exists(_.contains(userId))

  override def countFlags(eventId: EventId): Long = getFlaggers(eventId).size

  override def addFlag(eventId: EventId, by: UserId): Boolean =
    eventById(eventId).exists { event =>
      val alreadyFlagged = flags.get(eventId).exists(_ contains by)
      if (alreadyFlagged) false
      else {
        flags(eventId) = getFlaggers(eventId) + by
        true
      }
    }

  private def getNextEventId: EventId = EventId(events.map(_.id).maxBy(_.value).value + 1)

  override def addEvent(event: Event): EventAddition = {
    val copiedEvent = event.copy(id = getNextEventId)
    events.synchronized(events ::= copiedEvent)
    Added()
  }
}
