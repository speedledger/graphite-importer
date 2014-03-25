package com.speedledger.measure.graphite.jenkins

import org.scalatest.{FreeSpec, Matchers, FunSuite}
import org.scalatest.concurrent.ScalaFutures
import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.testkit.TestActorRef
import com.speedledger.measure.graphite.{ElasticsearchActorTest, Update, GraphiteActorMock, ElasticsearchActorMock}
import scala.concurrent.Promise
import spray.http.HttpRequest
import org.joda.time.DateTime
import org.json4s._
import org.json4s.native.JsonMethods._
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Integration test on [[com.speedledger.measure.graphite.jenkins.JenkinsActor]] together with 
 * [[com.speedledger.measure.graphite.ElasticsearchActor]] and [[com.speedledger.measure.graphite.GraphiteActor]].
 * Mocks interaction with remote services.
 */
class JenkinsIntegrationTest extends FreeSpec with Matchers with ScalaFutures {
  implicit val system = ActorSystem("jenkins-integration-test")

  implicit val timeout = Timeout(1.second)
  implicit val patience = PatienceConfig(timeout = 1.second)

  val jenkinsConfig = ConfigFactory.parseString("""|query-time-adjustment = 1 milliseconds
                                                   |query-size = 100
                                                   |url = "http://graphite"""".stripMargin)

  "On update Jenkins should fetch data from Elasticsearch and sends measures to Graphite" - {
    val elasticRequest = Promise[HttpRequest]()
    TestActorRef(Props(new ElasticsearchActorMock(elasticRequest, ElasticsearchActorTest.httpResponseWithTestData)), "elasticsearch")

    val graphiteRequest = Promise[HttpRequest]()
    TestActorRef(Props(new GraphiteActorMock(graphiteRequest)), "graphite")

    val jenkins = TestActorRef(Props(new JenkinsActorMock(jenkinsConfig)))

    val queryTime = DateTime.now
    jenkins ! Update(queryTime)

    // The query sent to ES should use the query time.
    val json = parse(elasticRequest.future.futureValue.entity.asString)
    (json \\ "_timestamp").children.head shouldEqual JInt(queryTime.getMillis - 1)

    graphiteRequest.future.futureValue.entity.asString shouldEqual
      "jenkins.builds.Awesome_project.success.duration 26984 1392731047"
  }
}

class JenkinsActorMock(override val config: Config) extends JenkinsActor
