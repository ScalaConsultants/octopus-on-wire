package domain

import com.google.inject.Inject
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

class TrustedUserDao @Inject()(dbConfig: DbConfig) {

  import dbConfig.db

  val trustedUsers = TableQuery[TrustedUsers]

  def isUserTrusted(userId: UserId): Future[Boolean] = db.run {
    trustedUsers.filter(_.id === userId).exists.result
  }
}