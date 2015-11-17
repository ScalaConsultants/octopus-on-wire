package scalac.octopusonwire.shared.domain

case class Message(text: String)

case object Invalid {
  val prefix = "Invalid: "
  def apply(arg: String): Message = new Message(prefix + arg)
  def unapply(mess: Message): Option[String] =
    if(mess.text.startsWith(prefix))
      Some(mess.text.drop(prefix.length))
    else None
}

case object Success{
  val prefix = "Success"
  def apply(): Message = new Message(prefix)
  def unapply(mess: Message): Boolean = mess.text == prefix
}