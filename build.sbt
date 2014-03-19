import AssemblyKeys._

organization  := "com.speedledger.measure"

name          := "graphite-importer"

version       := "0.1"

scalaVersion  := "2.10.3"

libraryDependencies ++= {
  val sprayVersion = "1.2.1"
  val akkaVersion = "2.2.4"
  val json4sVersion = "3.2.7"
  Seq(
    "io.spray"            %  "spray-can"     % sprayVersion,
    "io.spray"            %  "spray-client"  % sprayVersion,
    "io.spray"            %  "spray-http"    % sprayVersion,
    "io.spray"            %  "spray-httpx"   % sprayVersion,
    "io.spray"            %  "spray-util"    % sprayVersion,
    "io.spray"            %  "spray-testkit" % sprayVersion % "test",
    "com.typesafe.akka"   %% "akka-actor"    % akkaVersion,
    "com.typesafe.akka"   %% "akka-slf4j"    % akkaVersion,
    "com.typesafe.akka"   %% "akka-testkit"  % akkaVersion % "test",
    "org.json4s"          %% "json4s-native" % json4sVersion,
    "org.scalatest"       %% "scalatest"     % "2.1.0" % "test",
    "com.typesafe"        %  "config"        % "1.2.0",
    "ch.qos.logback"      %  "logback-classic" % "1.1.1",
    "com.github.nscala-time" %% "nscala-time" % "0.8.0"
  )
}

assemblySettings
