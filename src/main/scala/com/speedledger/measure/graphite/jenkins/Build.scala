package com.speedledger.measure.graphite.jenkins

case class Build(jobName: String, duration: Long, startTime: Long, result: String)
