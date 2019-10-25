import Dependencies._
import build._

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val protocol = Project(
  id = "todomvc-protocol",
  base = file("protocol")
).settings(
  commonSettings,
  Seq(
    libraryDependencies ++= circe ++ scalatest,
    scalafmtOnCompile := true
  )
)

lazy val server = Project(
  id = "todomvc-server",
  base = file("server")
).settings(
    commonSettings,
    Seq(
      libraryDependencies ++= akka ++ `akka-testkit` ++ circe ++ scalatest ++ tapir ++ runtimeLogging,
      scalafmtOnCompile := true,
      Test / fork := true
    )
  )
  .dependsOn(protocol)
  .aggregate(protocol)
