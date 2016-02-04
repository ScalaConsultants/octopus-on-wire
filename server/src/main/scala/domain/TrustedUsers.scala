package domain

import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.UserId

case class TrustedUser(id: UserId)

class TrustedUsers(tag: Tag) extends Table[TrustedUser](tag, "trusted_users") {
  def id = column[UserId]("id", O.PrimaryKey)

  override def * : ProvenShape[TrustedUser] = id <>(TrustedUser.apply, TrustedUser.unapply)
}

object TrustedUsers {
  val db = DbConfig.db
  val users = TableQuery[TrustedUsers]

  def isUserTrusted(userId: UserId): Future[Boolean] = db.run {
    users.filter(_.id === userId).exists.result
  }
}