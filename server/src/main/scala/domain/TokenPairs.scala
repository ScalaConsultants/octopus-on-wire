package domain

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

object TokenPairs {
  def saveUserToken(token: String, user: UserInfo): Unit = db.run {
    tokens.insertOrUpdate(TokenPair(token, user.userId))
  }

  val db = DbConfig.db

  def userIdByToken(token: String)(implicit ec: ExecutionContext): Future[UserId] = db.run {
    tokens.filter(_.token === token).map(_.userId).result
  }.flatMap {
    case Seq(id) => Future.successful(id)
    case _ => Future.failed(new Exception("User id not found"))
  }

  val tokens = TableQuery[TokenPairs]
}