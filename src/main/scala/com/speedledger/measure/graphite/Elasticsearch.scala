package com.speedledger.measure.graphite

import spray.client.pipelining._
import org.json4s._
import org.json4s.JsonAST.{JArray, JObject, JValue}
import org.json4s.native._
import akka.actor.{ActorLogging, Actor}
import com.typesafe.config.ConfigFactory
import scala.util.{Success, Failure}
import Utils._
import Utils.Pipeline._

object ElasticsearchActor {

  /**
   * Query that should be sent to Elasticsearch.
   *
   * @param indexName name of index.
   * @param typeName name of type.
   * @param query optional query on Elasticsearchs query DSL
   *              [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl.html]]
   */
  case class Query(indexName: String, typeName: String, query: Option[JValue])

  case class Response(objects: Seq[JObject])

}

/**
 * Actor that performs queries on Elasticsearch and returns the result to the sender.
 * Communicates with Elasticsearchs HTTP API.
 */
class ElasticsearchActor extends Actor with ActorLogging with JsonSupport {

  import ElasticsearchActor._

  import context.dispatcher

  val config = ConfigFactory.load().getConfig("elasticsearch")

  val url = config.getString("url")
  val credentials = config.getStringOption("authorization-credentials")

  def sendAndReceive = sendReceive
  val pipeline = addOptionalBasicAuthorization(credentials) ~> (sendAndReceive ~> unmarshal[JValue])

  def receive = {
    case Query(indexName, typeName, query) =>
      val originalSender = context.sender
      val uri = s"$url/$indexName/$typeName/_search"
      log.debug("Searching Elasticsearch on '{}' with query {}", uri, query.map(q => prettyJson(renderJValue(q))))

      pipeline(Get(uri, query)) onComplete {
        case Success(response) =>
          val objects = extractSourceObjects(response)
          log.info(s"Read ${objects.length} objects from $indexName/$typeName")
          originalSender ! Response(objects)
        case Failure(ex) =>
          log.error(ex, "Error when getting data from elasticsearch")
      }
  }

  def extractSourceObjects(response: JsonAST.JValue): List[JObject] = {
    val hits = response \ "hits" \ "hits"
    for {
      JArray(list) <- hits
      JObject(hit) <- list
      JField("_source", source: JObject) <- hit
    } yield source
  }
}
