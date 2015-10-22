package scalac.octopusonwire.shared

import scalac.octopusonwire.shared.domain.Event

trait Api {
  def getItems(limit: Int): Array[Event]
}