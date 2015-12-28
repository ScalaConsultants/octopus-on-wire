package scalac.octopusonwire.shared.domain

/**
  * Unique distinction between users.
  * Equal to Github user entity's "id" field.
  **/
case class UserId(value: Long)

object NoUserId extends UserId(-1)
case class UserInfo(userId: UserId, login: String)