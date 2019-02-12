name := "MoodifyAPI"

version := "0.1"

scalaVersion := "2.12.8"

val akkaVersion     = "2.5.20"
val akkaHttpVersion = "10.1.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka"            %% "akka-actor"                % akkaVersion,
  "com.typesafe.akka"            %% "akka-stream"               % akkaVersion,
  "com.typesafe.akka"            %% "akka-http"                 % akkaHttpVersion,
  "com.typesafe.akka"            %% "akka-http-spray-json"      % akkaHttpVersion,
  "com.typesafe.scala-logging"   %% "scala-logging"             % "3.9.0",
  "net.debasishg"                %% "redisclient"               % "3.9",
  "org.scalatest"                %% "scalatest"                 % "3.0.5" % "test",
  "ch.qos.logback"               %  "logback-classic"           % "1.2.3",
  "se.michaelthelin.spotify"     %  "spotify-web-api-java"      % "2.0.5",
  "com.typesafe"                 %  "config"                    % "1.3.2",
  "org.slf4j"                    %  "slf4j-simple"              % "1.6.2" % Test
)

mainClass in (Compile, packageBin) := Some("moodify.api.Boot")

mainClass in (Compile, run) := Some("moodify.api.Boot")

assemblyJarName in assembly := s"${name.value}.jar"
