package com.speedledger.measure.graphite

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.io.Source
import org.json4s.JsonAST._
import spray.http._
import com.speedledger.measure.graphite.ElasticsearchActor._
import scala.Some

object ElasticsearchActorTest {
  val testData = Source.fromURL(getClass.getResource("elasticsearch.testdata.json")).getLines().mkString

  val httpResponseWithTestData = HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, testData))
}

/**
 * Tests for [[ElasticsearchActor]].
 */
class ElasticsearchActorTest extends TestKit(ActorSystem("elasticsearch-actor-test"))
with ImplicitSender with FreeSpecLike with Matchers with ScalaFutures with Inside {

  "Query is sent and response is transformed" - {
    val httpRequest = Promise[HttpRequest]()
    val httpResponse = ElasticsearchActorTest.httpResponseWithTestData

    val elasticsearch = TestActorRef(Props(new ElasticsearchActorMock(httpRequest, httpResponse)))
    within(1 second) {
      val query = JObject("size" -> JInt(1))
      elasticsearch ! Query("index", "type", Some(query))

      val response = expectMsgClass(classOf[Response])
      response.objects should have length 1
    }

    inside(httpRequest.future.futureValue) {
      case HttpRequest(method, uri, _, entity, _) =>
        method.name shouldEqual "GET"
        uri.toString should include("/index/type")
        entity.asString shouldEqual """{"size":1}"""
    }
  }

  "Source objects are extracted" - {
    val elasticsearchRef = TestActorRef[ElasticsearchActor]
    val elasticsearch = elasticsearchRef.underlyingActor

    val A = JObject("name" -> JString("A"))
    val B = JObject("name" -> JString("B"))

    val data =
      JObject("hits" ->
        JObject("hits" ->
          JArray(List(
            JObject("_source" -> A),
            JObject("_source" -> B)))))

    elasticsearch.extractSourceObjects(data) should contain theSameElementsInOrderAs List(A, B)
  }
}

class ElasticsearchActorMock(request: Promise[HttpRequest], response: HttpResponse) extends ElasticsearchActor {
  // Capture the HTTP request and mock the result
  override def sendAndReceive = in => {
    request.success(in)
    Future.successful(response)
  }
}