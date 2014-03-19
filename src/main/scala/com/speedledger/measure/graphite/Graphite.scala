package com.speedledger.measure.graphite

import spray.client.pipelining._
import scala.util.{Success, Failure}
import akka.actor.{ActorLogging, Actor}
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import akka.event.LoggingAdapter
import Utils._
import Utils.Pipeline._

/**
 * Measurement to be sent to Graphite.
 *
 * @param path the path that the measurement should be recorded on.
 * @param value the value that should be recorded.
 * @param time at which time the measurement was done, in milliseconds since epoch.
 */
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
 *
 * Sends data for each measure on format "path value time", several measures are separated by a newline.
 * 'path' is a dot delimited path, 'value' is an integer or decimal and 'time' is number of seconds sine epoch.
 */
trait GraphiteHTTP {
  self: Actor =>
  def log: LoggingAdapter

  val config = ConfigFactory.load().getConfig("graphite")

  val url = config.getString("url")
  val credentials = config.getStringOption("authorization-credentials")

  val pipeline = addOptionalBasicAuthorization(credentials) ~> (sendReceive ~> unmarshal[String])

  def sendMeasures(measures: Seq[Measure]): Unit = {
    if (measures.isEmpty) {
      log.info("No data to send to Graphite")
    } else {
      val data = prepareData(measures)
      log.debug("Measurement data: {}", data)
      pipeline(Post(url, data)) onComplete {
        case Failure(ex) => log.error(ex, "Error when sending data to Graphite")
        case Success("") => log.info(s"Sent ${measures.size} measurements")
        case Success(response) => log.warning("Got unexpected response from Graphite: {}", response)
      }
    }
  }

  def prepareData(measures: Seq[Measure]) = {
    measures map {
      measure =>
        val epochSeconds = measure.time / 1000
        s"${measure.dotPath} ${measure.value} $epochSeconds"
    } mkString "\n"
  }
}
