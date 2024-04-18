import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "CS553",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.18",
      "com.typesafe.akka" % "akka-actor-typed_2.13" % "2.6.18",
      "com.typesafe.akka" % "akka-stream-typed_2.13" % "2.6.18",
      "com.typesafe.akka" % "akka-http_2.13" % "10.2.4",
      "com.typesafe.akka" % "akka-http-spray-json_2.13" % "10.2.4",
      "ch.qos.logback" % "logback-classic" % "1.0.9",
      "com.typesafe" % "config" % "1.2.1"
    )
  )