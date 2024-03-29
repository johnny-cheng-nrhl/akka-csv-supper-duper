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
    Dependencies.akkaActor,
    Dependencies.akkaTestKit,
    Dependencies.alpakkaCsv,
    Dependencies.alpakkaFile,
    Dependencies.nScalaTime,
    Dependencies.alpakkaJson,
    Dependencies.circeCore,
    Dependencies.circeGeneric,
    Dependencies.circeParser,
    Dependencies.scalaLogging,
    Dependencies.logbackCore,
    Dependencies.logbackClassic,
    Dependencies.mysqlConnector,
    Dependencies.doobieCore,
    Dependencies.doobieHikari,
    Dependencies.doobieH2,
    Dependencies.doobieScalaTest,
    Dependencies.doobieSpec2,
    Dependencies.parSequence,
    Dependencies.scalaTest
  ),
  scalacOptions ++= Seq(
    "-language:higherKinds", // Allow higher-kinded types
    "-Ypartial-unification"
  )
)
