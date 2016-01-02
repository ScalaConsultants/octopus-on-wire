package domain

import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class Users(tag: Tag) extends Table[UserInfo](tag, "users") {
  def id = column[UserId]("id", O.PrimaryKey)

  def login = column[String]("login")

  override def * : ProvenShape[UserInfo] = toTuple <>(UserInfo.tupled, UserInfo.unapply)

  def toTuple = (id, login)
}

object Users {
  def saveUserInfo(userInfo: UserInfo): Unit = db.run{
    users.insertOrUpdate(userInfo)
  }

  def userById(id: UserId): Future[Option[UserInfo]] = db.run{
    users.filter(_.id === id).result.headOption
  }

  val db = DbConfig.db
  val users = TableQuery[Users]
}