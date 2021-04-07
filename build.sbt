val catsVersion           = "2.5.0"
val catsEffectVersion     = "3.0.1"
val fs2Version            = "3.0.1"
val scodecBitsVersion     = "1.1.25"
val scodecCoreVersion     = "1.11.7"
val scodecStreamVersion   = "3.0.0-RC1"
val shapelessVersion      = "2.3.3"
val specs2Version         = "4.10.6"
val tikaVersion           = "1.24"
val scalacheckVersion     = "1.14.1"
val catsEffectTestVersion = "0.5.2"
val log4catsVersion       = "1.2.2"
val logbackVersion        = "1.2.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.minosiants",
    name := "pencil",
    scalaVersion := "2.12.13",
    crossScalaVersions := Seq("2.12.13", "2.13.5"),
    scalacOptions ++= Seq(
      "-language:experimental.macros",
      "-Yrangepos",
      "-Ywarn-unused",
      "-Xlint"
    ),
    javacOptions ++= Seq("-source", "1.11", "-target", "1.8"),
    libraryDependencies ++= Seq(
      "org.typelevel"     %% "cats-core"                  % catsVersion,
      "org.typelevel"     %% "cats-effect-kernel"         % catsEffectVersion,
      "co.fs2"            %% "fs2-core"                   % fs2Version,
      "co.fs2"            %% "fs2-io"                     % fs2Version,
      "com.chuusai"       %% "shapeless"                  % shapelessVersion,
      "org.scodec"        %% "scodec-bits"                % scodecBitsVersion,
      "org.scodec"        %% "scodec-core"                % scodecCoreVersion,
      "org.typelevel"     %% "log4cats-core"              % log4catsVersion,
      "org.apache.tika"   % "tika-core"                   % tikaVersion,
      "org.scala-lang"    % "scala-reflect"               % scalaVersion.value,
      "org.specs2"        %% "specs2-core"                % specs2Version % Test,
      "org.specs2"        %% "specs2-scalacheck"          % specs2Version % Test,
      "org.scalacheck"    %% "scalacheck"                 % scalacheckVersion %Test,
      "com.codecommit"    %% "cats-effect-testing-specs2" % catsEffectTestVersion % Test,
      "org.scodec"        %% "scodec-stream"              % scodecStreamVersion % Test,
      "ch.qos.logback"    % "logback-classic"             % logbackVersion % Test,
      "org.typelevel"     %% "log4cats-slf4j"             % log4catsVersion % Test
    ),
    addCompilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full
    ),
    addCompilerPlugin(scalafixSemanticdb),
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
