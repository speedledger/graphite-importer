package com.speedledger.measure.graphite

import org.scalatest.{Inside, Matchers, FunSuite}
import org.scalatest.concurrent.ScalaFutures
import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import akka.testkit.TestActorRef
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.io.Source
import org.json4s.JsonAST.JObject
import spray.http._
import spray.http.HttpRequest
import spray.http.HttpResponse
import org.json4s.JsonAST.JInt
import scala.Some
import com.speedledger.measure.graphite.ElasticsearchActor._

object ElasticsearchActorTest {
  val testData = Source.fromURL(getClass.getResource("elasticsearch.testdata.json")).getLines().mkString

  val httpResponseWithTestData = HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, testData))
}

/**
 * Tests for [[ElasticsearchActor]].
 */
class ElasticsearchActorTest extends FunSuite with Matchers with ScalaFutures with Inside {
  implicit val system = ActorSystem("elasticsearch-actor-test")

  implicit val timeout = Timeout(1.second)

  test("Query is sent and response is transformed") {
    val httpRequest = Promise[HttpRequest]()
    val httpResponse = ElasticsearchActorTest.httpResponseWithTestData

    val elasticsearch = TestActorRef(Props(new ElasticsearchActorMock(httpRequest, httpResponse)))

    val query = JObject("size" -> JInt(1))
    val response = (elasticsearch ? Query("index", "type", Some(query))).mapTo[Response]

    inside(httpRequest.future.futureValue) {
      case HttpRequest(method, uri, _, entity, _) =>
        method.name shouldEqual "GET"
        uri.toString should include("/index/type")
        entity.asString shouldEqual """{"size":1}"""
    }

    response.futureValue.objects should have length 1
  }
}

class ElasticsearchActorMock(request: Promise[HttpRequest], response: HttpResponse) extends ElasticsearchActor {
  // Grab the HTTP request and mock the result
  override def sendAndReceive = in => {
    request.success(in)
    Future.successful(response)
  }
}