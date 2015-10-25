package services

import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.Event

class ApiService extends Api {
  override def getItems(limit: Int): Array[Event] = {
    Array(
      Event(1, "Warsaw Scala FortyFives - Scala Application Development #scala45pl", 1445672700000L, 1445672700000L+3600000*8, "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw", "http://www.meetup.com/WarszawScaLa/events/225320171/"),
      Event(2, "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", 1445672700000L+3600000*24*2, 1445672700000L+3600000*24*3, "Wydział MIMUW, Banacha 2, Warsaw", "http://www.meetup.com/Poland-CodiLime-Tech-Talk/events/226054818/"),
      Event(3, "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", 1447591200000L, 1447591200000L, "Javeo, Stadion Narodowy, Warsaw", "http://www.meetup.com/WarszawScaLa/events/226075320/"),
      Event(4, "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", 1447591200000L, 1447591200000L, "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226099814/"),
      Event(5, "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", 1447591200000L, 1447591200000L, "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/225901253/"),
      Event(6, "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup", 1447591200000L, 1447591200000L, "Fujitsu Rnd Łódź, Fabryczna 17, Łódź", "http://www.meetup.com/Java-User-Group-Lodz/events/226169070/")
    )
  }
}
