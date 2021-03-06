package com.speedledger.measure.graphite

import akka.actor.{Props, ActorSystem}
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import akka.event.Logging

/**
 * Application that imports data into Graphite.
 */
object GraphiteImporter extends App {
  val config = ConfigFactory.load()

  val system = ActorSystem()
  val log = Logging.getLogger(system, this)

  val elasticsearch = system.actorOf(Props[ElasticsearchActor], "elasticsearch")
  val graphite = system.actorOf(Props[GraphiteActor], "graphite")

  val updater = system.actorOf(Props[UpdaterActor], "updater")


  val interval = FiniteDuration(config.getDuration("updater.interval", SECONDS), SECONDS)
  log.info(s"Update interval is $interval")

  system.scheduler.schedule(initialDelay = 1.second, interval, updater, Tick)
}
