import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.Version

name := "sbt-resolver"

lazy val metadataSettings = Seq(

  organization := "com.here.platform.artifact",

  projectInfo := ModuleInfo(
    nameFormal = "HERE SBT Resolver for Workspace and Marketplace",
    description = "The SBT Resolver is a Sbt plugin that can be referenced from the build.sbt of a Sbt project in order to consume/publish artifacts to the OLP Artifact storage.",
    homepage = Some(uri("http://here.com")),
    startYear = Some(2019),
    licenses = Vector(),
    organizationName = "HERE Europe B.V",
    organizationHomepage = Some(uri("http://here.com")),
    scmInfo = Some(ScmInfo(
      connection = "scm:git:https://github.com/heremaps/here-artifact-sbt-resolver.git",
      devConnection = "scm:git:git@github.com:heremaps/here-artifact-sbt-resolver.git",
      browseUrl = uri("https://github.com/heremaps/here-artifact-sbt-resolver")
    )),
    developers = Vector(Developer(
      "here",
      "HERE Artifact Service Team",
      "ARTIFACT_SERVICE_SUPPORT@here.com",
      url = uri("https://github.com/heremaps")
    ))
  )
)

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(metadataSettings)

crossScalaVersions := Seq("2.12.20", "3.8.4")

pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.11.7"
    case "3"    => "2.0.6"
  }
}

scriptedLaunchOpts := {
  scriptedLaunchOpts.value ++
    Seq("-Dplugin.version=" + version.value)
}

libraryDependencies ++= Seq(
  "com.here.account" % "here-oauth-client" % "0.4.20",
  "com.lihaoyi" %% "ujson" % "4.4.3",
  "org.scalatest" %% "scalatest" % "3.2.20" % Test,
  "org.scalamock" %% "scalamock" % "7.5.5" % Test
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

releaseTagComment :=
  s"Release ${(ThisBuild / version).value} from build ${sys.env.getOrElse("TRAVIS_BUILD_ID", "None")}"

releaseNextVersion := {
  ver =>
    Version(sys.props.getOrElse("currentVersion", ver))
      .map(_.bump(releaseVersionBump.value).string)
      .getOrElse(sbtrelease.versionFormatError(ver))
}

commands += Command.command("prepareRelease")((state: State) => {
  println("Preparing release...")
  val projectState = Project.extract(state)
  val customState = projectState.appendWithoutSession(
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
    state
  )
  Command.process("release with-defaults", customState)
})