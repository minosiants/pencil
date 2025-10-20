val catsVersion = "2.13.0"
val catsEffectVersion = "3.6.3"
val fs2Version = "3.12.2"
val scodecBitsVersion = "1.2.4"
val scodecCoreVersion = "2.3.3"
val scodecStreamVersion = "3.0.1"
val specs2Version = "5.6.4"
val tikaVersion = "3.2.3"
val scalacheckVersion = "1.19.0"
val log4catsVersion = "2.7.1"
val logbackVersion = "1.5.13"
val literallyVersion = "1.2.0"
val http4sVersion = "0.23.32"
val circeVersion = "0.14.15"

ThisBuild / scalafixScalaBinaryVersion := (ThisBuild / scalaBinaryVersion).value
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision


lazy val root = (project in file("."))
  .settings(
    organization := "com.minosiants",
    name := "pencil",
    scalaVersion := "3.3.6",
    scalacOptions ++= Seq(
      "-language:experimental.macros",
      "-new-syntax",
      "-indent",
      "-source:future",
      "-deprecation",
      "-feature"
    ),
    javacOptions ++= Seq("-source", "1.17", "-target", "1.17"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "literally" % literallyVersion,
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "co.fs2" %% "fs2-scodec" % fs2Version % Test,
      "org.scodec" %% "scodec-core" % scodecCoreVersion,
      "org.scodec" %% "scodec-bits" % scodecBitsVersion,
      "org.typelevel" %% "log4cats-core" % log4catsVersion,
      "org.apache.tika" % "tika-core" % tikaVersion,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion % Test,
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-scalacheck" % specs2Version % Test,
      "org.testcontainers" % "testcontainers" % "2.0.0" % Test,
      "org.fusesource.jansi" % "jansi" % "2.4.2" % Test,
      "org.http4s" %% "http4s-ember-client" % http4sVersion % Test,
      "org.http4s" %% "http4s-dsl" % http4sVersion % Test,
      "org.http4s" %% "http4s-circe" % http4sVersion % Test,
      "io.circe" %% "circe-core" % circeVersion % Test,
      "io.circe" %% "circe-generic" % circeVersion % Test,
      "io.circe" %% "circe-parser" % circeVersion % Test
    ),
    publishTo := sonatypePublishToBundle.value
  )
  .settings(releaseProcessSettings)
  .settings(licenceSettings)

import ReleaseTransformations._
lazy val releaseProcessSettings = Seq(
  releaseIgnoreUntrackedFiles := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseCrossBuild := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+ publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val licenceSettings = Seq(
  organizationName := "Kaspar Minosiants",
  startYear := Some(2020),
  licenses += ("Apache-2.0", new URL(
    "https://www.apache.org/licenses/LICENSE-2.0.txt"
  ))
)
