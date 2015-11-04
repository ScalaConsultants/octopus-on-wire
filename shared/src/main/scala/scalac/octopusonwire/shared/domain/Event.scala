package scalac.octopusonwire.shared.domain

case class Event(id: Long, name: String, startDate: Long, endDate: Long, location: String, url: String) {
  def toSimple: SimpleEvent = SimpleEvent(id, name)
}

case class SimpleEvent(id: Long, name: String)