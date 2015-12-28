package domain

import domain.Mappers._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class UserDao(tag: Tag) extends Table[UserInfo](tag, "users") {
  def id = column[UserId]("id", O.PrimaryKey, O.AutoInc)(UserIdMapper)

  def login = column[String]("login")

  override def * : ProvenShape[UserInfo] = toTuple <>(UserInfo.tupled, UserInfo.unapply)

  def toTuple = (id, login)
}



object UserDao{
  val users = TableQuery[UserDao]
}