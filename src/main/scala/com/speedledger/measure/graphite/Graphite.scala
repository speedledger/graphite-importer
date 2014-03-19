package com.speedledger.measure.graphite

import spray.client.pipelining._
import scala.util.{Success, Failure}
import akka.actor.{ActorLogging, Actor}
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import akka.event.LoggingAdapter
import Utils._
import Utils.Pipeline._

case class Measure(path: Seq[String], value: Long, time: EpochMilliseconds) {
  def cleanPath = {
    val removeParentheses = (in: String) => in.replaceAll("[\\(\\)]", "")
    val replaceSeparators = (in: String) => in.replaceAll("[ \\.]", "_")
    val clean = removeParentheses compose replaceSeparators

    path.map(clean)
  }

  def dotPath = cleanPath.mkString(".")
}

case class Measures(measures: Seq[Measure])

/**
 * Actor that sends measurements to Graphite.
 */
class GraphiteActor extends Actor with ActorLogging with GraphiteHTTP {
  def receive = {
    case Measures(measures) =>
      sendMeasures(measures)
    case measure: Measure =>
      sendMeasures(Seq(measure))
  }
}

/**
 * HTTP implementation to communicate with Graphite.
 */
trait GraphiteHTTP {
  self: Actor =>
  def log: LoggingAdapter

  val config = ConfigFactory.load().getConfig("graphite")

  val url = config.getString("url")
  val credentials = config.getStringOption("authorization-credentials")

  val pipeline = addOptionalBasicAuthorization(credentials) ~> (sendReceive ~> unmarshal[String])

  def prepareData(measures: Seq[Measure]) = {
    measures map {
      case m@Measure(_, value, time) =>
        val epochSeconds = time / 1000
        s"${m.dotPath} $value $epochSeconds"
    } mkString "\n"
  }

  def sendMeasures(measures: Seq[Measure]) = {
    val data = prepareData(measures)
    log.debug("Measurement data: {}", data)
    pipeline(Post(url, data)) onComplete {
      case Failure(ex) => log.error(ex, "Error when sending data to Graphite")
      case Success("") => log.info(s"Sent ${measures.size} measurements")
      case Success(response) => log.warning("Got unexpected response from Graphite: {}", response)
    }
  }
}
