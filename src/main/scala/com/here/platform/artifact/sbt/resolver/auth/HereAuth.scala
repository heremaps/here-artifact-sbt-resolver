/*
 * Copyright (C) 2019-2025 HERE Europe B.V.
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

import com.here.account.auth.provider.FromProperties
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import com.here.account.http.HttpProvider
import com.here.account.http.apache.ApacheHttpClientProvider
import com.here.account.oauth2.{ClientCredentialsGrantRequest, HereAccount}
import com.here.account.util.SettableSystemClock

object HereAuth {

  private val OAUTH_CONNECTION_TIMEOUT_IN_MS = 20000
  private val OAUTH_REQUEST_TIMEOUT_IN_MS = 20000

  private val hereCredentials = Credentials.loadHereCredentials()

  private var token: String = _

  def getTokenEndpointUrl: String = hereCredentials.tokenEndpointUrl

  def getToken: String = {
    if (token == null) {
      val tokenEndpoint = HereAccount.getTokenEndpoint(
        createHttpProvider(),
        new FromProperties(new SettableSystemClock(),
          hereCredentials.tokenEndpointUrl,
          hereCredentials.accessKeyId,
          hereCredentials.accessKeySecret,
          hereCredentials.scope.orNull)
      )
      token = tokenEndpoint.requestToken(new ClientCredentialsGrantRequest).getAccessToken
    }
    token
  }

  def getUser: Option[String] = hereCredentials.userId

  /**
    * Creates the HttpProvider to use for HereAccount.
    * This must add in any proxy settings that sbt is aware of
    * and forward them into the underlying http client
    *
    */
  def createHttpProvider(): HttpProvider = {
    val requestConfigBuilder = RequestConfig.custom
      .setConnectTimeout(OAUTH_CONNECTION_TIMEOUT_IN_MS)
      .setConnectionRequestTimeout(OAUTH_REQUEST_TIMEOUT_IN_MS)
    val clientBuilder =
      HttpClientBuilder.create.useSystemProperties
        .setDefaultRequestConfig(requestConfigBuilder.build)
    ApacheHttpClientProvider.builder
      .setHttpClient(clientBuilder.build)
      .setDoCloseHttpClient(true)
      .build
  }

}
