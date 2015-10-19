package spatutorial.shared

trait Api {
  def getItems(limit: Int): Array[Event]
}

case class Event(id: Int, name: String)