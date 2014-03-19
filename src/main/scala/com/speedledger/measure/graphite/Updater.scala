package com.speedledger.measure.graphite

import org.joda.time.DateTime
import akka.actor.{Props, Actor}
import com.typesafe.config.ConfigFactory
import com.speedledger.measure.graphite.jenkins.JenkinsActor
import java.io.File
import scala.util.Try
import com.speedledger.measure.graphite.Utils.FileIO

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
