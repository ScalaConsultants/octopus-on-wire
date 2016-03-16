package io.scalac.octopus.server

import org.mockito.stubbing.OngoingStubbing
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

trait OctoSpec extends FlatSpec with ScalaFutures with ShouldMatchers {

  implicit class Lel[T](stub: OngoingStubbing[Future[T]]) {
    def thenReturnFuture(t: T) = stub.thenReturn(Future.successful(t))
  }

}