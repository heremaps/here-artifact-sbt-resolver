scalaVersion := "2.12.20"

resolvers += "HERE_PLATFORM_ARTIFACT" at "here+artifact-service://artifact-service"

lazy val verifyResolver = taskKey[Unit]("Verify the HERE artifact resolver URL handler is installed")

verifyResolver := {
  val url = new java.net.URI("here+artifact-service://artifact-service").toURL
  assert(url.getProtocol == "here+artifact-service")
  streams.value.log.info("HERE artifact resolver URL handler is installed")
}
