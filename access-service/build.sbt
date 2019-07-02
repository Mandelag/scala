ThisBuild / scalaVersion := "2.12.7"
ThisBuild / organization := "com.mandelag"

lazy val app = (project in file("."))
  .settings(
    name := "UseService",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.5.23",
      "com.typesafe.akka" %% "akka-stream" % "2.5.23",
      "com.typesafe.akka" %% "akka-remote" % "2.5.23",
      "com.typesafe" % "config" % "1.3.3",
      "com.github.tototoshi" %% "scala-csv" % "1.3.6"
    )
  )

enablePlugins(JavaAppPackaging)
  
