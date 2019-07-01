ThisBuild / scalaVersion := "2.12.7"
ThisBuild / organization := "com.mandelag"

lazy val app = (project in file("."))
  .settings(
    name := "CharCounter",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.23",
      "com.typesafe.akka" %% "akka-stream" % "2.5.23",
      "com.typesafe.akka" %% "akka-remote" % "2.5.23",
      "com.typesafe.akka" %% "akka-cluster" % "2.5.23",
      "com.typesafe" % "config" % "1.3.3"
    )
  )

enablePlugins(JavaAppPackaging)
  