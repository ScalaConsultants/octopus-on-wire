package services

import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.Event

class ApiService extends Api {
  override def getItems(limit: Int): Array[Event] = {
    Array(
      Event(1, "Warsaw Scala FortyFives - Scala Application Development #scala45pl", 1445672700000L, 1445701500000L, "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw", "http://www.meetup.com/WarszawScaLa/events/225320171/"),
      Event(2, "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", 1446053400000L, 1446058800000L, "Wydział MIMUW, Banacha 2, Warsaw", "http://www.meetup.com/Poland-CodiLime-Tech-Talk/events/226054818/"),
      Event(3, "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", 1446656400000L, 1446665400000L, "Javeo, Stadion Narodowy, Warsaw", "http://www.meetup.com/WarszawScaLa/events/226075320/"),
      Event(4, "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", 1445529600000L, 1445536800000L, "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226099814/"),
      Event(5, "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", 1446138000000L, 1446202800000L, "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/225901253/"),
      Event(6, "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup", 1447489800000L, 1447517700000L, "Fujitsu Rnd Łódź, Fabryczna 17, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226169070/")
    )
  }
}
