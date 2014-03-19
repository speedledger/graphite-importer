package com.speedledger.measure.graphite.jenkins

import akka.actor.{ActorLogging, Actor}
import com.speedledger.measure.graphite._
import com.speedledger.measure.graphite.Measure
import org.json4s.JsonDSL._
import com.github.nscala_time.time.Implicits._
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit

/**
 * Actor that extracts Jenkins builds from Elasticsearch and reports creates measurements that are sent to Graphite.
 * Requires the builds to be indexed with the _timestamp field.
 */
class JenkinsActor extends Actor with ActorLogging with JsonSupport {
  val elasticsearch = context.actorSelection("/user/elasticsearch")
  val graphite = context.actorSelection("/user/graphite")

  val config = ConfigFactory.load().getConfig("updater.jenkins")

  def receive = {
    case Update(lastTime) =>
      val timeAdjustment = config.getDuration("query-time-adjustment", TimeUnit.MILLISECONDS)
      val fromTime = lastTime - timeAdjustment
      val query =
        ("size" -> config.getLong("query-size")) ~
          ("query" ->
            ("range" ->
              ("_timestamp" ->
                ("gte" -> fromTime.millis))))

      log.info("Querying Elasticsearch for builds after {}", fromTime)
      elasticsearch ! ElasticsearchActor.Query("jenkins", "build", Some(query))
    case ElasticsearchActor.Response(objects) =>
      val builds = objects.map(_.extractOpt[Build]).flatten
      log.info("Parsed {} builds out of {} objects", builds.size, objects.size)
      val measures = builds flatMap createMeasures
      graphite ! Measures(measures)
  }

  def createMeasures(build: Build) = {
    val pathBase = Seq("jenkins", "builds", build.jobName)

    val measures = Map(
      "duration" -> build.duration
    )

    measures.map {
      case (keyName, value) =>
        val resultKey = build.result.toLowerCase
        val path = pathBase :+ resultKey :+ keyName
        Measure(path, value, build.startTime)
    }
  }
}
