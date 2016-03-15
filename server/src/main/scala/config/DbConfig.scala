package config

import com.google.inject.{Singleton, Inject}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.JdbcBackend

@Singleton
class DbConfig @Inject()(configProvider: DatabaseConfigProvider) {
  val dbConfig = configProvider.get[JdbcProfile]
  val db: JdbcBackend#DatabaseDef = {
    dbConfig.db
  }
}