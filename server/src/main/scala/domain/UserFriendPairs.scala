package domain

import com.google.inject.Inject
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

class UserFriendPairDao @Inject()(dbConfig: DbConfig) {
  import dbConfig.db

  def saveUserFriends(userId: UserId, friends: Set[UserId]): Future[Unit] = db.run {
    userFriendPairs ++= friends.map(UserFriendPair(userId, _))
  }.map(_ => ())

  def getUserFriends(userId: UserId): Future[Set[UserId]] = db.run {
    userFriendPairs.filter(_.userId === userId).map(_.friendId).result
  }.map(_.toSet)

  val userFriendPairs = TableQuery[UserFriendPairs]
}