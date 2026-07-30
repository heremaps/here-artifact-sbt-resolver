# sbt 1 / sbt 2 cross-build notes

This repository supports publishing two sbt plugin artifacts from the same codebase:

| Target  | Published artifact        |
| ------- | ------------------------- |
| sbt 2.x | `sbt-resolver_sbt2_3`     |
| sbt 1.x | `sbt-resolver-1_2.12_1.0` |

## How it works

Most of the implementation is shared and lives under:

```text
src/main/scala
```

This includes:

* ArtifactURLHandler
* HttpUtils
* ArtifactPropertiesResolver
* HereAuth
* Connection handlers
* Retry logic
* Tests

Only the sbt-specific plugin wiring differs between sbt 1 and sbt 2:

```text
src/main/sbt-1/scala/com/here/platform/artifact/sbt/resolver/ArtifactResolverPlugin.scala
src/main/sbt-2/scala/com/here/platform/artifact/sbt/resolver/ArtifactResolverPlugin.scala
```

The build selects the appropriate plugin implementation based on the configured sbt target version while reusing the shared implementation.

## Local validation

### sbt 2

Compile, test and publish locally:

```bash
sbt 'set pluginCrossBuild / sbtVersion := "2.0.0-RC15"; set scalaVersion := "3.8.4"; clean; test; publishLocal'
```

Expected artifact:

```text
~/.ivy2/local/com.here.platform.artifact/sbt-resolver/...
```

Published plugin coordinates:

```scala
addSbtPlugin(
  "com.here.platform.artifact" % "sbt-resolver" % "<version>"
)
```

### sbt 1

Compile, test and publish locally:

```bash
sbt 'set pluginCrossBuild / sbtVersion := "1.11.3"; set scalaVersion := "2.12.20"; clean; test; publishLocal'
```

Expected artifact:

```text
~/.ivy2/local/com.here.platform.artifact/sbt-resolver-1/...
```

Published plugin coordinates:

```scala
addSbtPlugin(
  "com.here.platform.artifact" % "sbt-resolver" % "<version>"
)
```