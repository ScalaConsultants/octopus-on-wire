package io.scalac.octopus.server

import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future

trait OctoSpec extends FlatSpec with ScalaFutures with ShouldMatchers with MockitoSugar {

  implicit class StubSuccessfulFuture[T](stub: OngoingStubbing[Future[T]]) {
    def thenReturnFuture(t: T) = stub.thenReturn(Future.successful(t))
  }

  def mockDeep[T <: AnyRef](implicit manifest: Manifest[T]): T = {
    Mockito.mock(manifest.runtimeClass.asInstanceOf[Class[T]], RETURNS_DEEP_STUBS)
  }

}