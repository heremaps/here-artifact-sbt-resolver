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

import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpUriRequest}
import org.apache.http.util.EntityUtils

import java.net.HttpURLConnection._
import scala.util.parsing.json.JSON

/**
  * Resolves schema hrn prefix and default artifact service url based on here token url.
  */
object ArtifactPropertiesResolver {

  private val TOKEN_PROD_URL = "https://account.api.here.com/oauth2/token"

  private val TOKEN_STAGING_URL = "https://stg.account.api.here.com/oauth2/token"

  private val API_LOOKUP_PROD_URL = "https://api-lookup.data.api.platform.here.com/lookup/v1"

  private val API_LOOKUP_STAGING_URL = "https://api-lookup.data.api.platform.sit.here.com/lookup/v1"

  private val TOKEN_CN_PROD_URL = "https://account.hereapi.cn/oauth2/token"

  private val TOKEN_CN_STAGING_URL = "https://account.sit.hereapi.cn/oauth2/token"

  private val API_LOOKUP_CN_PROD_URL = "https://api-lookup.data.api.platform.hereolp.cn/lookup/v1/"

  private val API_LOOKUP_CN_STAGING_URL = "https://api-lookup.data.api.platform.in.hereolp.cn/lookup/v1/"

  // Regional domains are used until the services up on the target domains
  private val TOKEN_CN_REGIONAL_PROD_URL = "https://elb.cn-northwest-1.account.hereapi.cn/oauth2/token"

  private val TOKEN_CN_REGIONAL_STAGING_URL = "https://elb.cn-northwest-1.account.sit.hereapi.cn/oauth2/token"

  private val URL_MAPPING = initializeArtifactServiceUrlsMap

  private def initializeArtifactServiceUrlsMap =
    Map(
      TOKEN_PROD_URL -> API_LOOKUP_PROD_URL,

      TOKEN_STAGING_URL -> API_LOOKUP_STAGING_URL,
      TOKEN_CN_PROD_URL -> API_LOOKUP_CN_PROD_URL,
      TOKEN_CN_STAGING_URL -> API_LOOKUP_CN_STAGING_URL,

      TOKEN_CN_REGIONAL_PROD_URL -> API_LOOKUP_CN_PROD_URL,
      TOKEN_CN_REGIONAL_STAGING_URL -> API_LOOKUP_CN_STAGING_URL,
    )

  /**
    * Resolves schema default artifact service url based on here token url.
    *
    * @param tokenUrl here token url
    * @return resolved default artifact service url
    */

  def resolveArtifactServiceUrl(tokenUrl: String, requestExecutor: HttpUriRequest => CloseableHttpResponse): String = {
    val artifactApiLookupUrl = getApiLookupUrl(tokenUrl) + "/platform/apis/artifact/v1"
    val httpGet = new HttpGet(artifactApiLookupUrl)
    val response = requestExecutor.apply(httpGet)
    val statusCode = response.getStatusLine.getStatusCode
    val content = EntityUtils.toString(response.getEntity)
    statusCode match {
      case HTTP_OK | HTTP_CREATED => validatedAndParse(content)
      case _ => throw new RuntimeException("Unable to resolve Artifact Service URL. Status: " + response.getStatusLine.getReasonPhrase)
    }
  }

  private def getApiLookupUrl(tokenUrl: String) = {
    val endpoint = tokenUrl.trim
    URL_MAPPING.getOrElse(endpoint,
      throw new IllegalArgumentException(s"Unknown token endpoint: $endpoint"))
  }

  private def validatedAndParse(content: String) = {
    val result = JSON.parseFull(content)
    result match {
      case Some(list: List[Map[String, Any]]) => {
        val last = list.last
        last("baseURL").toString + "/artifact"
      }
      case None => throw new IllegalArgumentException("Parsing failed")
      case other => throw new IllegalArgumentException(s"Unknown data structure: $other")
    }
  }
}
