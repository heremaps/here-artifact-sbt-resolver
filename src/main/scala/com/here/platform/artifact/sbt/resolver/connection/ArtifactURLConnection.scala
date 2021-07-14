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

package com.here.platform.artifact.sbt.resolver.connection

import java.io.InputStream
import java.net.{HttpURLConnection, URL}

import com.here.platform.artifact.sbt.resolver.utils.HttpUtils._
import org.apache.http.client.methods.{HttpGet, HttpHead}
import org.apache.http.message.BasicLineFormatter.formatStatusLine

/**
  * Implements an HttpURLConnection for compatibility with Coursier (https://github.com/coursier/coursier)
  */
final class ArtifactURLConnection(url: URL) extends HttpURLConnection(url) {

  private[this] lazy val response = {
    val artifact = toArtifact(url.toString)
    val groupHrnPrefix = registerExists(artifact.groupId, artifact.artifactId).groupHrnPrefix
    val resolvedUrl = rewriteUrl(groupHrnPrefix, artifact)

    val response = executeRequest(getRequestMethod.toLowerCase match {
      case "head" => new HttpHead(resolvedUrl)
      case "get" => new HttpGet(resolvedUrl)
      case _ =>
        throw new IllegalArgumentException(s"Unexpected request method [$getRequestMethod].")
    })

    connected = true
    response
  }

  override def connect(): Unit = ()

  override def getInputStream: InputStream = response.getEntity.getContent

  override def getHeaderField(n: Int): String =
    // n == 0 means you want the HTTP Status Line
    // This is called from HttpURLConnection.getResponseCode()
    if (n == 0) formatStatusLine(response.getStatusLine, null)
    else super.getHeaderField(n)

  override def getHeaderField(field: String): String = Option(response.getFirstHeader(field)).map(_.getValue).orNull

  override def disconnect(): Unit = response.close()

  override def usingProxy(): Boolean =
    sys.env.get("http.proxyHost").exists {
      _ != ""
    } || sys.env.get("https.proxyHost").exists {
      _ != ""
    }
}
