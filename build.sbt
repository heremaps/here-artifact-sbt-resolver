import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.Version

name := "sbt-resolver"

lazy val metadataSettings = Seq(

  organization := "com.here.platform.artifact",

  projectInfo := ModuleInfo(
    nameFormal = "HERE SBT Resolver for Workspace and Marketplace",
    description = "The SBT Resolver is a Sbt plugin that can be referenced from the build.sbt of a Sbt project in order to consume/publish artifacts to the OLP Artifact storage.",
    homepage = Some(url("http://here.com")),
    startYear = Some(2019),
    licenses = Vector(),
    organizationName = "HERE Europe B.V",
    organizationHomepage = Some(url("http://here.com")),
    scmInfo = Some(ScmInfo(
      connection = "scm:git:https://github.com/heremaps/here-artifact-sbt-resolver.git",
      devConnection = "scm:git:git@github.com:heremaps/here-artifact-sbt-resolver.git",
      browseUrl = url("https://github.com/heremaps/here-artifact-sbt-resolver")
    )),
    developers = Vector(Developer("here", "HERE Artifact Service Team", "ARTIFACT_SERVICE_SUPPORT@here.com", url = url("https://github.com/heremaps")))
  )
)

lazy val root = (project in file("."))
  .settings(metadataSettings: _*)

sbtPlugin := true

crossScalaVersions := Seq("2.11", "2.12")

libraryDependencies ++= Seq(
  "com.here.account" % "here-oauth-client" % "0.4.20",
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,
  "org.scalamock" %% "scalamock" % "4.4.0" % Test
)

useGpgAgent := false
useGpgPinentry := true
sonatypeProfileName := "com.here"
publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
pomIncludeRepository := { _ => false }

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

releaseTagName := (version in ThisBuild).value
releaseTagComment := s"Release ${(version in ThisBuild).value} from build ${sys.env.getOrElse("TRAVIS_BUILD_ID", "None")}"
releaseNextVersion := {
  ver => Version(sys.props.getOrElse("currentVersion", ver))
    .map(_.bump(releaseVersionBump.value).string).getOrElse(sbtrelease.versionFormatError(ver))
}

commands += Command.command("prepareRelease")((state: State) => {
  println("Preparing release...")
  val projectState = Project extract state
  val customState = projectState.appendWithoutSession(Seq(releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    setNextVersion,
    runClean,
    runTest,
    tagRelease
  )), state)
  Command.process("release with-defaults", customState)
})
