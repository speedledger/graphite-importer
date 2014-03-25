package com.speedledger.measure.graphite

import org.scalatest.{FreeSpec, WordSpec, Matchers, FunSuite}
import akka.actor.{Props, ActorSystem}
import akka.testkit.TestActorRef
import scala.concurrent.{Promise, Future}
import spray.http.{HttpRequest, StatusCodes, HttpResponse}
import org.scalatest.concurrent.ScalaFutures

/**
 * Tests for [[com.speedledger.measure.graphite.GraphiteActor]].
 */
class GraphiteActorTest extends FreeSpec with Matchers with ScalaFutures {
  implicit val system = ActorSystem("graphite-actor-test")

  val FirstMeasure = Measure(Seq("a", "b"), 123, 123456789000L)
  val FirstMeasureAsString = "a.b 123 123456789"
  val SecondMeasure = Measure(Seq("a", "c"), 987, 987654321000L)
  val SecondMeasureAsString = "a.c 987 987654321"

  val BothMeasures = Measures(Seq(FirstMeasure, SecondMeasure))
  val BothMeasuresAsString = s"$FirstMeasureAsString\n$SecondMeasureAsString"

  "Measures can be sent" - {
    def fixture = new {
      val request = Promise[HttpRequest]()
      val graphite = TestActorRef(Props(new GraphiteActorMock(request)))
      def requestBody = request.future.futureValue.entity.asString
    }

    "Single measure" - {
      val f = fixture
      f.graphite ! FirstMeasure
      f.requestBody shouldEqual FirstMeasureAsString
    }

    "Multiple measures" - {
      val f = fixture
      f.graphite ! BothMeasures
      f.requestBody shouldEqual BothMeasuresAsString
    }
  }

  "Measures can be converted to string format" - {
    val graphiteRef = TestActorRef[GraphiteActor]
    val graphite = graphiteRef.underlyingActor

    "Single measure" - {
      graphite.prepareData(FirstMeasure) shouldEqual FirstMeasureAsString
    }

    "Multiple measures" - {
      graphite.prepareData(BothMeasures.measures) shouldEqual BothMeasuresAsString
    }
  }
}

class GraphiteActorMock(request: Promise[HttpRequest]) extends GraphiteActor {
  // Grab the HTTP request and mock the result
  override def sendAndReceive = in => {
    request.success(in)
    Future.successful(HttpResponse(status = StatusCodes.OK))
  }
}