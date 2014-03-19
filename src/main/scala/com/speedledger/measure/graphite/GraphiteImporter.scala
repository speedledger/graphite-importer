package com.speedledger.measure.graphite

import akka.actor.{Actor, Props, ActorSystem}
import org.json4s._
import spray.httpx.Json4sSupport
import com.speedledger.measure.graphite.jenkins.JenkinsActor
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import akka.event.Logging
import org.joda.time.DateTime
import com.speedledger.measure.graphite.Utils.FileIO
import java.io.File
import scala.util.Try

/**
 * Application that imports data into Graphite.
 */
object GraphiteImporter extends App with JsonSupport {
  val config = ConfigFactory.load()

  implicit val system = ActorSystem()
  val log = Logging.getLogger(system, this)

  val elasticsearch = system.actorOf(Props[ElasticsearchActor], "elasticsearch")
  val graphite = system.actorOf(Props[GraphiteActor], "graphite")

  val updater = system.actorOf(Props[UpdaterActor], "updater")


  val interval = FiniteDuration(config.getDuration("updater.interval", SECONDS), SECONDS)
  log.info(s"Update interval is $interval")

  system.scheduler.schedule(initialDelay = 1.second, interval, updater, Tick)
}

case object Tick

case class Update(lastUpdateTime: DateTime)

class UpdaterActor extends Actor {
  val config = ConfigFactory.load().getConfig("updater")

  val jenkins = context.actorOf(Props[JenkinsActor], "jenkins")

  val timeFile = new File(config.getString("next-query-time-file"))
  var nextQueryTime =
    Try(FileIO.read(timeFile).toLong).map(new DateTime(_))
      .getOrElse(new DateTime(0))

  def receive = {
    case Tick =>
      if (config.getBoolean("jenkins.enabled")) {
        jenkins ! Update(nextQueryTime)
      }

      nextQueryTime = DateTime.now
      FileIO.write(nextQueryTime.getMillis.toString, timeFile)
  }
}

trait JsonSupport extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats
}
