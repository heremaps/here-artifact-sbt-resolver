/*
 * Copyright (C) 2019-2020 HERE Europe B.V.
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

package com.here.platform.artifact.sbt.resolver.utils

/**
  * Resolves schema hrn prefix and default artifact service url based on here token url.
  */
object ArtifactPropertiesResolver {

  private val TOKEN_PROD_URL = "https://account.api.here.com/oauth2/token"
  private val TOKEN_STAGING_URL = "https://stg.account.api.here.com/oauth2/token"
  private val TOKEN_CN_PROD_URL =
    "https://elb.cn-northwest-1.account.hereapi.cn/oauth2/token"
  private val TOKEN_CN_STAGING_URL =
    "https://elb.cn-northwest-1.account.sit.hereapi.cn/oauth2/token"

  private val PROD_ARTIFACT_SERVICE_URL =
    "https://artifact.api.platform.here.com/v1/artifact"
  private val STAGING_ARTIFACT_SERVICE_URL =
    "https://artifact.api.platform.in.here.com/v1/artifact"
  private val CN_PROD_ARTIFACT_SERVICE_URL =
    "https://artifact.api.platform.hereolp.cn/v1/artifact"
  private val CN_STAGING_ARTIFACT_SERVICE_URL =
    "https://artifact.api.platform.in.hereolp.cn/v1/artifact"

  private val ARTIFACT_SERVICE_URLS_MAP = initializeArtifactServiceUrlsMap

  /**
    * Resolves schema default artifact service url based on here token url.
    *
    * @param tokenUrl here token url
    * @return resolved default artifact service url
    */
  def resolveArtifactServiceUrl(tokenUrl: String): String =
    ARTIFACT_SERVICE_URLS_MAP.getOrElse(
      tokenUrl,
      throw new IllegalArgumentException(String.format("Unknown token endpoint: %s", tokenUrl)))

  private def initializeArtifactServiceUrlsMap =
    Map(
      TOKEN_PROD_URL -> PROD_ARTIFACT_SERVICE_URL,
      TOKEN_STAGING_URL -> STAGING_ARTIFACT_SERVICE_URL,
      TOKEN_CN_PROD_URL -> CN_PROD_ARTIFACT_SERVICE_URL,
      TOKEN_CN_STAGING_URL -> CN_STAGING_ARTIFACT_SERVICE_URL
    )
}
