import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.Version

ThisBuild / organization := "com.here.platform.artifact"
ThisBuild / version := "2.0.4-SNAPSHOT"

name := "sbt-resolver"

lazy val sbt2Version = "2.0.0-RC15"
lazy val sbt1Version = "1.11.3"
lazy val scala3ForSbt2 = "3.8.4"
lazy val scala212ForSbt1 = "2.12.20"

def isSbt2(version: String): Boolean =
  version.startsWith("2.")

lazy val metadataSettings = Seq(
  description := "The SBT Resolver is an sbt plugin for consuming/publishing artifacts to the HERE Artifact storage.",
  homepage := Some(uri("https://here.com")),
  startYear := Some(2019),
  licenses := Seq(License.Apache2),
  organizationName := "HERE Europe B.V",
  organizationHomepage := Some(uri("https://here.com")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/heremaps/here-artifact-sbt-resolver"),
      "scm:git:https://github.com/heremaps/here-artifact-sbt-resolver.git",
      Some("scm:git:git@github.com:heremaps/here-artifact-sbt-resolver.git")
    )
  ),
  developers := List(
    Developer(
      "here",
      "HERE Artifact Service Team",
      "ARTIFACT_SERVICE_SUPPORT@here.com",
      url("https://github.com/heremaps")
    )
  )
)

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(metadataSettings)
  .settings(
    moduleName := "sbt-resolver",
    pluginCrossBuild / sbtVersion := sbt2Version,
    scalaVersion := scala3ForSbt2,
    Compile / unmanagedSourceDirectories += {
      val sourceBase = baseDirectory.value / "src" / "main"
      if (isSbt2((pluginCrossBuild / sbtVersion).value)) sourceBase / "sbt-2" / "scala"
      else sourceBase / "sbt-1" / "scala"
    },
    libraryDependencies ++= Seq(
      "com.here.account" % "here-oauth-client" % "0.4.20",
      "com.lihaoyi" %% "ujson" % "4.4.3",
      "org.scalatest" %% "scalatest" % "3.2.20" % Test,
      "org.scalamock" %% "scalamock" % "7.5.5" % Test
    )
  )

useGpgAgent := false
useGpgPinentry := true
publishMavenStyle := true
sbtPluginPublishLegacyMavenStyle := false
pomIncludeRepository := { _ => false }
publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

pomExtra :=
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>

// Defines the release process
releaseIgnoreUntrackedFiles := true
releaseTagName := (ThisBuild / version).value
releaseTagComment := s"Release ${(ThisBuild / version).value} from build ${sys.env.getOrElse("TRAVIS_BUILD_ID", "None") }"
releaseNextVersion := { ver =>
  Version(sys.props.getOrElse("currentVersion", ver))
    .map(_.bump(releaseVersionBump.value).string)
    .getOrElse(sbtrelease.versionFormatError(ver))
}

commands += Command.command("prepareRelease") { (s: State) =>
  println("Preparing release...")
  val extracted = Project.extract(s)
  val customState = extracted.appendWithoutSession(
    Seq(
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        setNextVersion,
        runClean,
        runTest,
        tagRelease
      )
    ),
    s
  )
  Command.process("release with-defaults", customState)
}
