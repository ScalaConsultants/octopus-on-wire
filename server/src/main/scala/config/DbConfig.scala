package config

import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

object DbConfig {
  private val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val db: JdbcBackend#DatabaseDef = dbConfig.db
}