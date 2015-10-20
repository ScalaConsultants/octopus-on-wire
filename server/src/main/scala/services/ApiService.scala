package services

import spatutorial.shared._

class ApiService extends Api {
  override def getItems(limit: Int): Array[Event] = {
    Array(
      "Very long event name",
      "GeeCon 2015",
      "JDD 2015",
      "MyAwesomeEvent 2015"
    ).zipWithIndex.map{ pair =>
      Event(pair._2, pair._1)
    }
  }
}
