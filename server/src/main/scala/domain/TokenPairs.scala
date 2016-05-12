package domain

import com.google.inject.Inject
import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.{ExecutionContext, Future}
import scalac.octopusonwire.shared.domain.{UserInfo, TokenPair, UserId}

class TokenPairs(tag: Tag) extends Table[TokenPair](tag, "tokens") {
  def token = column[String]("token")

  def userId = column[UserId]("user_id", O.PrimaryKey)

  override def * : ProvenShape[TokenPair] = toTuple <>(TokenPair.tupled, TokenPair.unapply)

  def toTuple = (token, userId)
}

class TokenPairDao @Inject()(dbConfig: DbConfig) {

  import dbConfig.db

  def saveUserToken(token: String, userId: UserId): Future[Int] = db.run {
    tokens.insertOrUpdate(TokenPair(token, userId))
  }

  def userIdByToken(token: String)(implicit ec: ExecutionContext): Future[Option[UserId]] = db.run {
    tokens.filter(_.token === token).map(_.userId).result
  }.map(_.headOption)

  val tokens = TableQuery[TokenPairs]
}