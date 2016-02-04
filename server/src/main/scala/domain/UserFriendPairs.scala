package domain

import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserFriendPair, UserId}

class UserFriendPairs(tag: Tag) extends Table[UserFriendPair](tag, "user_friends") {
  def userId = column[UserId]("user_id")

  def friendId = column[UserId]("friend_id")

  override def * : ProvenShape[UserFriendPair] = toTuple <>(UserFriendPair.tupled, UserFriendPair.unapply)

  def toTuple = (userId, friendId)
}

object UserFriendPairs {
  def saveUserFriends(userId: UserId, friends: Set[UserId]): Unit = db.run {
    userFriendPairs ++= friends.map(UserFriendPair(userId, _))
  }

  val db = DbConfig.db

  /**
    * @return `None` if at least one of these is true for `userId`:
    *         <ul>
    *         <li>no friends have been cached</li>
    *         <li>no friends were cached by the time they were fetched</li>
    *         </ul>
    *         a `Set` of `UserId`s otherwise.
    **/
  def getUserFriends(userId: UserId): Future[Option[Set[UserId]]] = db.run {
    userFriendPairs.filter(_.userId === userId).map(_.friendId).result
  }.map { friends =>
    friends.isEmpty match {
      case true => None
      case _ => Some(friends.toSet)
    }
  }

  val userFriendPairs = TableQuery[UserFriendPairs]
}