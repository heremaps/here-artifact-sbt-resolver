/*
 * Copyright (C) 2019-2024 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.platform.artifact.sbt.resolver.auth

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.{ConfigFactory, ConfigParseOptions, ConfigSyntax}

sealed trait Credentials

final case class HereAccountCredentials(
    userId: Option[String],
    clientId: String,
    accessKeyId: String,
    accessKeySecret: String,
    tokenEndpointUrl: String,
    scope: Option[String]
) extends Credentials

object Credentials {

  private val userHomeSysProp = "user.home"
  private val dotHereDir = ".here"
  private val credentialsFileName = "credentials.properties"
  private val credentialsPropertyName = "hereCredentialsFile"
  private val credentialsEnvName = "HERE_CREDENTIALS_FILE"

  private val userIdKey = "here.user.id"
  private val clientIdKey = "here.client.id"
  private val accessKeyIdKey = "here.access.key.id"
  private val accessSecretKey = "here.access.key.secret"
  private val urlEndPointKey = "here.token.endpoint.url"
  private val scopeKey = "here.token.scope"

  def loadHereCredentials(): HereAccountCredentials = {
    val homeDir = System.getProperty(userHomeSysProp)
    val defaultPath = Paths.get(homeDir, dotHereDir, credentialsFileName)

    val systemPropertyFile = Option(System.getProperty(credentialsPropertyName)).orElse(Option(System.getenv(credentialsEnvName)))
    val path = systemPropertyFile.map(Paths.get(_)).getOrElse(defaultPath)
    parsePropertyFile(path.toFile)
  }

  private def parsePropertyFile(file: File): HereAccountCredentials = {
    val fileConfig = ConfigFactory.parseFile(
      file,
      ConfigParseOptions.defaults().setSyntax(ConfigSyntax.PROPERTIES))
    HereAccountCredentials(
      userId =
        if (fileConfig.hasPath(userIdKey))
          Some(fileConfig.getString(userIdKey))
        else None,
      clientId = fileConfig.getString(clientIdKey),
      accessKeyId = fileConfig.getString(accessKeyIdKey),
      accessKeySecret = fileConfig.getString(accessSecretKey),
      tokenEndpointUrl = fileConfig.getString(urlEndPointKey),
      scope = if (fileConfig.hasPath(scopeKey)) Some(fileConfig.getString(scopeKey)) else None
    )
  }
}
