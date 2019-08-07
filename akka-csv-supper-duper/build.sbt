lazy val akkaVersion = "2.5.22"
lazy val alpakkaVersion = "1.0.2"
lazy val nscalaVersion = "2.20.0"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.hautelook.catalog",
      scalaVersion := "2.12.6",
      version := "0.1.3-SNAPSHOT"
    )
  ),
  name := "akka-quickstart-scala",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % alpakkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-csv" % alpakkaVersion,
    "com.lightbend.akka" %% "akka-stream-alpakka-file" % alpakkaVersion,
    // Nscala Time
    "com.github.nscala-time" %% "nscala-time" % nscalaVersion,
    "org.scalatest" %% "scalatest" % "3.0.5" % "test"
  ),
  scalacOptions ++= Seq(
    "-language:higherKinds", // Allow higher-kinded types
    "-Ypartial-unification"
  )
)

// set the main class for the main 'sbt run' task
mainClass in (Compile, run) := Some("net.propoint.fun.CsvConsumer")
