package services

import scalac.octopusonwire.shared.Api
import scalac.octopusonwire.shared.domain.Event

class ApiService extends Api {
  override def getItems(limit: Int): Array[Event] = {
    Array(
      Event(1, "Warsaw Scala FortyFives - Scala Application Development #scala45pl", 1447591200, 1447591200, "Politechnika Warszawska Wydział Matematyki i Nauk Informacyjnych, Koszykowa 75, Warsaw"),
      Event(2, "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup", 1447591200, 1447591200, "Wydział MIMUW, Banacha 2, Warsaw"),
      Event(3, "Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup", 1447591200, 1447591200, "Javeo, Stadion Narodowy, Warsaw"),
      Event(4, "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup", 1447591200, 1447591200, "Sala A2, DMCS, ul. Wólczanska 221/223, budynek B18 90-924, Łódź"),
      Event(5, "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup", 1447591200, 1447591200, "Tektura Kawa & Bistro, Tymienieckiego 3, Łódź"),
      Event(6, "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup", 1447591200, 1447591200, "Fujitsu Rnd Łódź, Fabryczna 17, Łódź")
    )
  }
}
