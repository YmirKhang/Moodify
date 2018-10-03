
name := "MoodifyAPI"

version := "0.1"

scalaVersion := "2.12.7"

val akkaV       = "2.5.14"
val akkaHttpV   = "10.1.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka"            %% "akka-actor"                % akkaV,
  "com.typesafe.akka"            %% "akka-stream"               % akkaV,
  "com.typesafe.akka"            %% "akka-http"                 % akkaHttpV,
  "com.typesafe.akka"            %% "akka-http-spray-json"      % akkaHttpV,
  "com.typesafe.scala-logging"   %% "scala-logging"             % "3.8.0",
  "se.michaelthelin.spotify"     % "spotify-web-api-java"       % "2.0.5",
  "net.debasishg"                %% "redisclient"               % "3.5"
)
