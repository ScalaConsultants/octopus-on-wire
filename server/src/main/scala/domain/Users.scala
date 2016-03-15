package domain

import com.google.inject.Inject
import config.DbConfig
import slick.driver.PostgresDriver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.{ExecutionContext, Future}
import scalac.octopusonwire.shared.domain.{UserId, UserInfo}

class Users(tag: Tag) extends Table[UserInfo](tag, "users") {
  def id = column[UserId]("id", O.PrimaryKey)

  def login = column[String]("login")

  override def * : ProvenShape[UserInfo] = toTuple <>(UserInfo.tupled, UserInfo.unapply)

  def toTuple = (id, login)
}

class UserDao @Inject() (dbConfig: DbConfig){
  import dbConfig.db

  val users = TableQuery[Users]

  def saveUserInfo(userInfo: UserInfo): Unit = db.run{
    users.insertOrUpdate(userInfo)
  }

  def userById(id: UserId)(implicit ec: ExecutionContext): Future[UserInfo] = db.run {
    users.filter(_.id === id).result
  }.flatMap {
    case Seq(info) => Future.successful(info)
    case _ => Future.failed(new Exception("User info not found"))
  }
}