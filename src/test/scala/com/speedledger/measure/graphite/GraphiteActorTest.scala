package com.speedledger.measure.graphite

import org.scalatest.{Matchers, FunSuite}
import akka.actor.{Props, ActorSystem}
import akka.testkit.TestActorRef
import scala.concurrent.{Promise, Future}
import spray.http.{HttpRequest, StatusCodes, HttpResponse}
import org.scalatest.concurrent.ScalaFutures

/**
 * Tests for [[com.speedledger.measure.graphite.GraphiteActor]].
 */
class GraphiteActorTest extends FunSuite with Matchers with ScalaFutures {
  implicit val system = ActorSystem("graphite-actor-test")

  test("Single measure are sent correctly") {
    val request = Promise[HttpRequest]()

    val graphite = TestActorRef(Props(new GraphiteActorMock(request)))
    graphite ! Measure(Seq("a", "b"), 123, 123456789000L)

    val requestBody = request.future.futureValue.entity.asString
    requestBody should be === "a.b 123 123456789"
  }

  test("Multiple measures are sent correctly") {
    val request = Promise[HttpRequest]()

    val graphite = TestActorRef(Props(new GraphiteActorMock(request)))
    val measures = Measures(Seq(
      Measure(Seq("a", "b"), 123, 123456789000L),
      Measure(Seq("a", "c"), 987, 987654321000L)))
    graphite ! measures

    val requestEntity = request.future.futureValue.entity.asString
    requestEntity should be === "a.b 123 123456789\na.c 987 987654321"
  }
}

class GraphiteActorMock(request: Promise[HttpRequest]) extends GraphiteActor {
  // Grab the HTTP request and mock the result
  override def sendAndReceive = in => {
    request.success(in)
    Future.successful(HttpResponse(status = StatusCodes.OK))
  }
}