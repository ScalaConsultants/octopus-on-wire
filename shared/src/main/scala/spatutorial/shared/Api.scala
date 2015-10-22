package spatutorial.shared

import spatutorial.shared.domain.Event

trait Api {
  def getItems(limit: Int): Array[Event]
}