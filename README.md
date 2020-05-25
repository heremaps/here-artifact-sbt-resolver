[![Build Status](https://travis-ci.com/heremaps/here-artifact-sbt-resolver.svg?token=qChpbefwyQKBzgjbCQ4s&branch=master)](https://travis-ci.com/heremaps/here-artifact-sbt-resolver)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.here.platform.artifact/sbt-resolver/badge.svg)](https://search.maven.org/artifact/com.here.platform.artifact/sbt-resolver)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

# HERE SBT Resolver for Workspace and Marketplace (Unofficial)

## Introduction
The HERE platform SBT resolver plugin provides Java and Scala developers with access to HERE platform
artifacts via SBT. It uses your HERE platform credentials to generate tokens so that it can pull your
SBT project dependencies from the HERE platform.

This allows Marketplace and Workspace users to [fetch platform schemas](https://developer.here.com/olp/documentation/archetypes/dev_guide/topics/archetypes-schema.html).
In addition, the users can [fetch the Java / Scala Data Client Library](https://developer.here.com/olp/documentation/marketplace-consumer/user-guide/topics/get_catalog_data.html)
which offer access to data in the HERE Data API.

Go to [the HERE Developer portal](https://developer.here.com/products/open-location-platform) to learn more about the HERE platform.

## Limitation
The SBT resolver plugin is provided 'as is' and not officially part of Workspace or Marketplace.
Support is only provided via GitHub and not via regular HERE product support channels.

## Prerequisites
To access the libraries and schemas from the HERE platform, you need a HERE Workspace and/or a HERE Marketplace account.
If you don’t have an account yet, go to [Pricing and Plans](https://developer.here.com/pricing/open-location-platform) to apply for a free trial.

Once you have enabled your account you need to create the credentials and prepare your environment.
Workspace users can find corresponding guidance [in the documentation for Java and Scala developers]( https://developer.here.com/olp/documentation/sdk-developer-guide/dev_guide/topics/how-to-use-sdk.html).
Marketplace users can find instructions in the [Marketplace Consumer user guide](https://developer.here.com/olp/documentation/marketplace-consumer/user-guide/topics/get_catalog_data.html#register-app).

This version is compatible with:
 - Scala 2.11/2.12
 - SBT 1.x

## How to use it?
This SBT resolver plugin is published on [Maven Central](https://search.maven.org/artifact/com.here.platform.artifact/sbt-resolver)
so you can conveniently use it from your project.
The `sbt-resolver` plugin can be registered by adding an entry to `projects/plugins.sbt` file as follows:


    addSbtPlugin("com.here.platform.artifact" % "sbt-resolver" % sbtResolverVersion)


For example, to fetch the HERE Map Content - Topology Geometry - Protocol Buffers schema and the related Java and Scala bindings set the following dependencies:


    resolvers += "HERE_PLATFORM_ARTIFACT" at "here+artifact-service://artifact-service"

    libraryDependencies += "com.here.schema.rib" % "topology-geometry_v2_proto" % topologyGeometryVersion
    libraryDependencies += "com.here.schema.rib" % "topology-geometry_v2_java" % topologyGeometryVersion
    libraryDependencies += "com.here.schema.rib" % "topology-geometry_v2_scala" % topologyGeometryVersion


As a Marketplace user you can add this dependency for fetching the Java / Scala Data Client Library:


    resolvers += "HERE_PLATFORM_ARTIFACT" at "here+artifact-service://artifact-service"

    libraryDependencies += "com.here.platform.data.client" %% "data-client" % dataClientVersion


`here+artifact-service://artifact-service` is placeholder URL which will be replaced by plugin dynamically based on your credentials.
The latest versions of the Data Client Library and Schemas can be found in [SDK documentation](https://developer.here.com/olp/documentation/java-scala-dev/dev_guide/sdk-libraries.html).

#### Proxy Setup
To enable SBT and the HERE SBT resolver plugin to work behind a proxy,
you need to use standard `http_proxy` and `https_proxy` environment variables, like:
```shell
sbt -Dhttp.proxyHost=PROXY_HOST -Dhttp.proxyPort=PROXY_PORT -Dhttp.proxyUser=PROXY_USERNAME -Dhttp.proxyPassword=PROXY_PASSWORD
```
Alternatively, pass those by setting `JAVA_OPTS` in the environment variable, like:
```shell
export JAVA_OPTS="-Dhttp.proxyHost=PROXY_HOST -Dhttp.proxyPort=PROXY_PORT -Dhttp.proxyUser=PROXY_USERNAME -Dhttp.proxyPassword=PROXY_PASSWORD"
```

## License
Copyright (C) 2019-2020 HERE Europe B.V.

Unless otherwise noted in `LICENSE` files for specific files or directories, the [LICENSE](LICENSE) in the root applies to all content in this repository.
