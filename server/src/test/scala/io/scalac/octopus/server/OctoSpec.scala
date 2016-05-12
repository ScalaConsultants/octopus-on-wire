package io.scalac.octopus.server

import config.DbConfig
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.db.DBApi
import play.api.db.evolutions.{DatabaseEvolutions, Evolution, Evolutions, ThisClassLoaderEvolutionsReader}
import play.api.inject.guice.GuiceApplicationBuilder
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver

import scala.concurrent.Future

trait OctoSpec extends FlatSpec with ScalaFutures with ShouldMatchers with MockitoSugar {

  implicit class StubSuccessfulFuture[T](stub: OngoingStubbing[Future[T]]) {
    def thenReturnFuture(t: T) = stub.thenReturn(Future.successful(t))
  }

  def mockDeep[T <: AnyRef](implicit manifest: Manifest[T]): T = {
    Mockito.mock(manifest.runtimeClass.asInstanceOf[Class[T]], Mockito.RETURNS_DEEP_STUBS)
  }

}

object TestDb {
  lazy val appBuilder = new GuiceApplicationBuilder()
  lazy val injector = appBuilder.injector()
  lazy val databaseApi = injector.instanceOf[DBApi]

  lazy val database = databaseApi.database("test")

  //type annotation is required here
  val conf: DatabaseConfig[PostgresDriver] = DatabaseConfig.forConfig("slick.dbs.test")

  val db = conf.db
}

trait DbSpec extends BeforeAndAfterEach {
  self: Suite with MockitoSugar =>

  val db = TestDb.db

  val dbConfig = mock[DbConfig]
  Mockito.when(dbConfig.db).thenReturn(db)

  override protected def beforeEach(): Unit = {
    val dbEvolutions = new DatabaseEvolutions(TestDb.database, "")
    val evos: Seq[Evolution] = ThisClassLoaderEvolutionsReader.evolutions("default")
    val evolutions = dbEvolutions.scripts(evos)

    new DatabaseEvolutions(TestDb.database).evolve(evolutions, autocommit = true)
  }

  override protected def afterEach(): Unit = {
    Evolutions.cleanupEvolutions(TestDb.database)
  }
}
