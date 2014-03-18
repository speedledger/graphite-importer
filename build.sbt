import AssemblyKeys._

organization  := "com.speedledger.measure"

name          := "graphite-importer"

version       := "0.1"

scalaVersion  := "2.10.3"

libraryDependencies ++= {
  val sprayVersion = "1.2.0"
  val akkaVersion = "2.2.3"
  val json4sVersion = "3.2.6"
  Seq(
    "io.spray"            %  "spray-can"     % sprayVersion,
    "io.spray"            %  "spray-client"  % sprayVersion,
    "io.spray"            %  "spray-http"    % sprayVersion,
    "io.spray"            %  "spray-httpx"   % sprayVersion,
    "io.spray"            %  "spray-util"    % sprayVersion,
    "com.typesafe.akka"   %% "akka-actor"    % akkaVersion,
    "com.typesafe.akka"   %% "akka-slf4j"    % akkaVersion,
    "org.json4s"          %% "json4s-native" % json4sVersion,
    "com.typesafe"        %  "config"        % "1.2.0",
    "ch.qos.logback"      %  "logback-classic" % "1.1.1",
    "com.github.nscala-time" %% "nscala-time" % "0.8.0"
  )
}

assemblySettings
