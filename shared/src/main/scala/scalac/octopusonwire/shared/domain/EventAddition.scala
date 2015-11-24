package scalac.octopusonwire.shared.domain

sealed case class EventAddition(text: String)

case object Added{
  val prefix = "Success"
  def apply(): EventAddition = new EventAddition(prefix)
  def unapply(mess: EventAddition): Boolean = mess.text == prefix
}

case object FailedToAdd {
  val prefix = "Invalid: "
  def apply(arg: String): EventAddition = new EventAddition(prefix + arg)
  def unapply(mess: EventAddition): Option[String] =
    if(mess.text.startsWith(prefix))
      Some(mess.text.drop(prefix.length))
    else None

  val `User not logged in` = "User not logged in"
  val `Event starts in the past` = "Event starts in the past"
}
