package com.speedledger.measure.graphite.jenkins

import org.scalatest.{Matchers, FunSuite}
import org.scalatest.concurrent.ScalaFutures
import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.testkit.TestActorRef
import com.speedledger.measure.graphite.{ElasticsearchActorTest, Update, GraphiteActorMock, ElasticsearchActorMock}
import scala.concurrent.Promise
import spray.http.HttpRequest
import org.joda.time.DateTime

/**
 * Integration test on [[com.speedledger.measure.graphite.jenkins.JenkinsActor]] together with 
 * [[com.speedledger.measure.graphite.ElasticsearchActor]] and [[com.speedledger.measure.graphite.GraphiteActor]].
 * Mocks interaction with remote services.
 */
class JenkinsIntegrationTest extends FunSuite with Matchers with ScalaFutures {
  implicit val system = ActorSystem("jenkins-integration-test")

  implicit val timeout = Timeout(1.second)
  implicit val patience = PatienceConfig(timeout = 1.second)

  test("On update Jenkins grabs data from Elasticsearch and sends to Graphite") {
    TestActorRef(Props(new ElasticsearchActorMock(Promise[HttpRequest](), ElasticsearchActorTest.httpResponseWithTestData)), "elasticsearch")

    val graphiteRequest = Promise[HttpRequest]()
    TestActorRef(Props(new GraphiteActorMock(graphiteRequest)), "graphite")

    val jenkins = TestActorRef(Props[JenkinsActor])

    val queryTime = DateTime.now
    jenkins ! Update(queryTime)

    graphiteRequest.future.futureValue.entity.asString should be ===
      "jenkins.builds.Awesome_project.success.duration 26984 1392731047"
  }
}
