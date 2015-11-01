package services

import java.text.SimpleDateFormat
import java.util.Date

import services.ServerConfig._

import scala.language.implicitConversions
import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.Event


class ApiService extends Api {
  val items: Array[Event] = Array(
    Event(1, "Warsaw Scala FortyFives - Scala Application Development #scala45pl", "2015-10-24 09:45".getTime, "2015-10-24 17:45".getTime, "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw", "http://www.meetup.com/WarszawScaLa/events/225320171/"),
    Event(2, "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", "2015-10-28 18:30".getTime, "2015-10-28 20:00".getTime, "Wydział MIMUW, Banacha 2, Warsaw", "http://www.meetup.com/Poland-CodiLime-Tech-Talk/events/226054818/"),
    Event(3, "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", "2015-11-04 18:00".getTime, "2015-11-04 20:30".getTime, "Javeo, Stadion Narodowy, Warsaw", "http://www.meetup.com/WarszawScaLa/events/226075320/"),
    Event(4, "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", "2015-10-22 18:00".getTime, "2015-10-22 20:30".getTime, "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226099814/"),
    Event(5, "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", "2015-10-29 18:00".getTime, "2015-10-30 0:00".getTime, "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/225901253/"),
    Event(6, "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup", "2015-11-14 09:30".getTime, "2015-11-14 17:15".getTime, "Fujitsu Rnd Łódź, Fabryczna 17, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226169070/"),
    Event(7, "TSUG (we are coming back after holiday break!)", "2015-10-29 18:00".getTime, "2015-10-29 20:30".getTime, "Olivia Business Centre, Olivia FOUR, aleja Grunwaldzka 472a, Gdansk", "http://www.meetup.com/Tricity-Scala-Users-Group/events/225945602/"),
    Event(8, "Best Scala event", System.currentTimeMillis() + 3600000*24, System.currentTimeMillis() + 3600000 * 28, "Some nice place", "https://scalac.io")
  )

  override def getFutureItems(limit: Int): Array[Event] = {
    val now = System.currentTimeMillis()
    items.filter { event =>
      event.startDate > now || event.endDate > now
    } sortBy (_.startDate) take limit
  }

}

object ServerConfig {
  val TimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm")

  implicit private[services] def stringDateToDate(s: String): Date = TimeFormat.parse(s)
}