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

package com.here.platform.artifact.sbt.resolver.utils

import com.here.platform.artifact.sbt.resolver.UnitSpec
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.entity.StringEntity

class ArtifactPropertiesResolverTest extends UnitSpec {

  "ArtifactPropertiesResolver" should "return artifact service url" in {
    assert(
      "https://artifact.api.platform.here.com/v1/artifact" ==
        ArtifactPropertiesResolver.resolveArtifactServiceUrl(
          "https://account.api.here.com/oauth2/token", _ => mockLookupAPIResponse("https://artifact.api.platform.here.com/v1")))
    assert(
      "https://artifact.api.platform.sit.here.com/v1/artifact" ==
        ArtifactPropertiesResolver.resolveArtifactServiceUrl(
          "https://stg.account.api.here.com/oauth2/token", _ => mockLookupAPIResponse("https://artifact.api.platform.sit.here.com/v1")))
    assert(
      "https://artifact.api.platform.hereolp.cn/v1/artifact" ==
        ArtifactPropertiesResolver.resolveArtifactServiceUrl(
          "https://elb.cn-northwest-1.account.hereapi.cn/oauth2/token", _ => mockLookupAPIResponse("https://artifact.api.platform.hereolp.cn/v1")))
    assert(
      "https://artifact.api.platform.in.hereolp.cn/v1/artifact" ==
        ArtifactPropertiesResolver.resolveArtifactServiceUrl(
          "https://elb.cn-northwest-1.account.sit.hereapi.cn/oauth2/token", _ => mockLookupAPIResponse("https://artifact.api.platform.in.hereolp.cn/v1"))
    )
  }

  it should "throws an exception if no mapping found" in {
    assertThrows[IllegalArgumentException] {
      ArtifactPropertiesResolver.resolveArtifactServiceUrl("http://unknown/url", _ => mock[CloseableHttpResponse])
    }
  }

  private def mockLookupAPIResponse(mockedUrl: String): CloseableHttpResponse = {
    val response = mock[CloseableHttpResponse]
    val statusLineMock = mock[StatusLine]
    (statusLineMock.getStatusCode _).stubs().returning(200)
    (response.getStatusLine _).stubs().returning(statusLineMock)
    (response.getEntity _).stubs().returning(new StringEntity("[{\"baseURL\":\"" + mockedUrl + "\"}]"))
    response
  }
}
