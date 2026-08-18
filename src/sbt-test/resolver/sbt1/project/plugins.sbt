sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.here.platform.artifact" % "sbt-resolver" % x)
  case _ => sys.error(
    """|The system property 'plugin.version' is not defined.
       |Specify this property using scriptedLaunchOpts -Dplugin.version=...
       |""".stripMargin)
}
