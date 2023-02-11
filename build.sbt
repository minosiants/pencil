val catsVersion             = "2.9.0"
val catsEffectVersion       = "3.4.5"
val fs2Version              = "3.6.1"
val scodecBitsVersion       = "1.1.27"
val scodecCoreScala2Version = "1.11.10"
val scodecCoreScala3Version = "2.2.1"
val scodecStreamVersion     = "3.0.1"
val specs2Version           = "4.19.2"
val tikaVersion             = "1.24"
val scalacheckVersion       = "1.15.4"
val log4catsVersion         = "2.5.0"
val logbackVersion          = "1.2.3"
val literallyVersion        = "1.1.0"

ThisBuild / scalafixScalaBinaryVersion := (ThisBuild / scalaBinaryVersion).value
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val root = (project in file("."))
  .settings(
    organization := "com.minosiants",
    name := "pencil",
    scalaVersion := "2.12.12",
    crossScalaVersions := Seq("2.12.12", "2.13.6", "3.2.2"),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          Seq(
            "-language:experimental.macros",
            "-Ykind-projector"
          )
        case _ =>
          Seq(
            "-language:experimental.macros",
            "-Yrangepos",
            "-Ywarn-unused",
            "-Xlint",
            "-P:semanticdb:synthetics:on"
          )

      }
    },
    javacOptions ++= Seq("-source", "1.11", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "cats-core"         % catsVersion,
      "org.typelevel"   %% "cats-effect"       % catsEffectVersion,
      "org.typelevel"   %% "literally"         % literallyVersion,
      "co.fs2"          %% "fs2-core"          % fs2Version,
      "co.fs2"          %% "fs2-io"            % fs2Version,
      "org.scodec"      %% "scodec-bits"       % scodecBitsVersion,
      "org.typelevel"   %% "log4cats-core"     % log4catsVersion,
      "org.apache.tika" % "tika-core"          % tikaVersion,
      "org.scalacheck"  %% "scalacheck"        % scalacheckVersion % "test",
      "org.scodec"      %% "scodec-stream"     % scodecStreamVersion % "test",
      "ch.qos.logback"  % "logback-classic"    % logbackVersion % "test",
      "org.typelevel"   %% "log4cats-slf4j"    % log4catsVersion % "test",
      "org.specs2"      %% "specs2-core"       % specs2Version % "test",
      "org.specs2"      %% "specs2-scalacheck" % specs2Version % Test
    ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) =>
          Seq(
            "org.scodec" %% "scodec-core" % scodecCoreScala3Version
          )
        case _ =>
          Seq(
            "org.scodec"     %% "scodec-core"  % scodecCoreScala2Version,
            "org.scala-lang" % "scala-reflect" % scalaVersion.value,
            compilerPlugin(
              "org.typelevel" %% "kind-projector" % "0.13.0" cross CrossVersion.full
            ),
            compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
            compilerPlugin(scalafixSemanticdb)
          )
      }
    },
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
