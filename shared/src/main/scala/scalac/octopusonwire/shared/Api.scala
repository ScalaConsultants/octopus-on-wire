package scalac.octopusonwire.shared

import scalac.octopusonwire.shared.domain.Event

trait Api {
  def getFutureItems(limit: Int): Array[Event]
}