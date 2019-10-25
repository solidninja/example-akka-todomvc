import sbt._

object Dependencies {

  object Versions {
    val akka = "2.6.0-RC1"
    val `akka-persistence-inmemory` = "2.5.15.2"
    val circe = "0.12.2"
    val scalatest = "3.0.8"
    val tapir = "0.11.9"
  }

  val akka = Seq(
    "com.typesafe.akka" %% "akka-persistence-typed" % Versions.akka,
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % Versions.`akka-persistence-inmemory`
  )

  val `akka-testkit` = Seq(
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.akka % Test
  )

  val circe = Seq(
    "io.circe" %% "circe-literal" % Versions.circe,
    "io.circe" %% "circe-jawn" % Versions.circe,
    "io.circe" %% "circe-generic" % Versions.circe
  )

  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  )

  val tapir = Seq(
    "com.softwaremill.tapir" %% "tapir-core" % Versions.tapir,
    "com.softwaremill.tapir" %% "tapir-akka-http-server" % Versions.tapir,
    "com.softwaremill.tapir" %% "tapir-json-circe" % Versions.tapir,
    "com.softwaremill.tapir" %% "tapir-openapi-docs" % Versions.tapir,
    "com.softwaremill.tapir" %% "tapir-openapi-circe" % Versions.tapir,
    "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % Versions.tapir,
    "com.softwaremill.tapir" %% "tapir-swagger-ui-akka-http" % Versions.tapir
  )

  val runtimeLogging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime"
  )
}
