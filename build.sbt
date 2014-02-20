import AssemblyKeys._

organization  := "com.speedledger.measure"

name          := "graphite-importer"

version       := "0.1"

scalaVersion  := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

libraryDependencies ++= {
  val sprayV = "1.2.0"
  val akkaV = "2.2.3"
  val json4sV = "3.2.6"
  Seq(
    "io.spray"            %  "spray-can"     % sprayV,
    "io.spray"            %  "spray-client"  % sprayV,
    "io.spray"            %  "spray-http"    % sprayV,
    "io.spray"            %  "spray-httpx"   % sprayV,
    "io.spray"            %  "spray-util"    % sprayV,
    "com.typesafe.akka"   %% "akka-actor"    % akkaV,
    "com.typesafe.akka"   %% "akka-slf4j"    % akkaV,
    "org.json4s"          %% "json4s-native" % json4sV,
    "com.typesafe"        %  "config"        % "1.2.0",
    "ch.qos.logback"      %  "logback-classic" % "1.1.1",
    "com.github.nscala-time" %% "nscala-time" % "0.8.0"
  )
}

assemblySettings
