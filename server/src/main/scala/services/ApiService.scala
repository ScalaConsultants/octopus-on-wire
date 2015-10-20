package services

import spatutorial.shared._

class ApiService extends Api {
  override def getItems(limit: Int): Array[Event] = {
    Array(
      "Warsaw Scala FortyFives - Scala Application Development #scala45pl",
      "Spark: Wprowadzenie - Poland CodiLime Tech Talk (Warsaw) - Meetup",
      "[DRAFT] Let's Scala few Apache Spark apps together - part 4! - Warsaw Scala Enthusiasts (Warsaw) - Meetup",
      "Don't fear the Monad - Java User Group Łódź (Łódź) - Meetup",
      "JUGToberfest 2015 - Java User Group Łódź (Łódź) - Meetup",
      "Global Day of Code Retreat - Java User Group Łódź (Łódź) - Meetup"
    ).zipWithIndex.map{ pair =>
      Event(pair._2, pair._1)
    }
  }
}
