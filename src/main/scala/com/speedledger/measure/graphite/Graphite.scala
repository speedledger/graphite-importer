package com.speedledger.measure.graphite

import spray.client.pipelining._
import scala.util.{Success, Failure}
import akka.actor.{ActorLogging, Actor}
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import akka.event.LoggingAdapter
import Utils._
import Utils.Pipeline._

case class Measure(path: Seq[String], value: Long, time: EpochMilliseconds)

case class Measures(measures: Seq[Measure])

class GraphiteActor extends Actor with ActorLogging with HostedGraphiteHTTP {
  def receive = {
    case Measures(measures) =>
      sendMeasures(measures)
    case measure: Measure =>
      sendMeasures(Seq(measure))
  }
}

trait HostedGraphiteHTTP {
  self: Actor =>
  def log: LoggingAdapter

  val config = ConfigFactory.load().getConfig("graphite")

  val url = config.getString("url")
  val credentials = config.getStringOption("authorization-credentials")

  val pipeline = addOptionalBasicAuthorization(credentials) ~> (sendReceive ~> unmarshal[String])
  
  def prepareData(measures: Seq[Measure]) = {
    measures map {
      case Measure(path, value, time) =>
        val cleanPath = path.map(_.replaceAll("[\\(\\)]", "").replaceAll("[ \\.]", "_"))
        val dotPath = cleanPath.mkString(".")
        val epochSeconds = time / 1000
        s"$dotPath $value $epochSeconds"
    } mkString "\n"
  }

  def sendMeasures(measures: Seq[Measure]) = {
    val data = prepareData(measures)
    log.debug("Measurement data: {}", data)
    pipeline(Post(url, data)) onComplete {
      case Failure(ex) => log.error(ex, "Error when sending data to Graphite")
      case Success("") => log.info(s"Sent ${measures.size} measures")
      case Success(response) => log.warning("Got unexpected response from Graphite: {}", response)
    }
  }
}
