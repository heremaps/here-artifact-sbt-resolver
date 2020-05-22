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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import org.apache.http.client.methods.{HttpGet, HttpHead}
import com.here.platform.artifact.sbt.resolver.utils.HttpUtils._
import org.apache.http.Header
import org.apache.http.client.utils.DateUtils

/**
  * Implements an HttpURLConnection for compatibility with Coursier (https://github.com/coursier/coursier)
  */
final class ArtifactURLConnection(url: URL) extends HttpURLConnection(url) {

  private[this] var response: Option[ArtifactResponse] = None

  private trait ArtifactResponse extends AutoCloseable {
    def inputStream: Option[InputStream]
    def metadata: Array[Header]
  }

  private case class HEADResponse(url: String) extends ArtifactResponse {
    def inputStream: Option[InputStream] = None
    def metadata: Array[Header] = {
      val httpHead = new HttpHead(url)
      val response = executeRequest(httpHead)
      response.getAllHeaders
    }
    def close(): Unit = {}
  }

  private case class GETResponse(url: String) extends ArtifactResponse {
    def inputStream: Option[InputStream] = {
      val httpGet = new HttpGet(url)
      val response = executeRequest(httpGet)
      Some(response.getEntity.getContent)
    }
    def metadata: Array[Header] = {
      val httpHead = new HttpHead(url)
      val response = executeRequest(httpHead)
      response.getAllHeaders
    }
    def close(): Unit = None
  }

  override def connect(): Unit = {
    val artifact = toArtifact(url.toString)
    val groupHrnPrefix = registerExists(artifact.groupId, artifact.artifactId).groupHrnPrefix
    val resolvedUrl = rewriteUrl(groupHrnPrefix, artifact)

    response = getRequestMethod.toLowerCase match {
      case "head" => Option(HEADResponse(resolvedUrl))
      case "get" => Option(GETResponse(resolvedUrl))
      case _ =>
        throw new IllegalArgumentException(s"Unexpected request method [$getRequestMethod].")
    }

    responseCode = if (response.isEmpty) 404 else 200

    // Also set the responseMessage (an HttpURLConnection field) for better compatibility
    responseMessage = statusMessageForCode(responseCode)

    connected = true
  }

  override def getInputStream: InputStream = {
    if (!connected) connect()
    response.flatMap { _.inputStream }.orNull
  }

  override def getHeaderField(n: Int): String =
    // n == 0 means you want the HTTP Status Line
    // This is called from HttpURLConnection.getResponseCode()
    if (n == 0 && responseCode != -1) {
      s"HTTP/1.0 $responseCode ${statusMessageForCode(responseCode)}"
    } else {
      super.getHeaderField(n)
    }

  override def getHeaderField(field: String): String = {
    if (!connected) connect()

    field.toLowerCase match {
      case "content-type" =>
        response.map {
          _.metadata.find(_.getName.equalsIgnoreCase("content-type")).map(_.getValue).orNull
        }.orNull
      case "content-encoding" =>
        response.map {
          _.metadata.find(_.getName.equalsIgnoreCase("content-encoding")).map(_.getValue).orNull
        }.orNull
      case "content-length" =>
        response.map {
          _.metadata.find(_.getName.equalsIgnoreCase("content-length")).map(_.getValue).orNull
        }.orNull
      case "last-modified" =>
        response
          .map {
            _.metadata.find(_.getName.equalsIgnoreCase("last-modified")).map(_.getValue).orNull
          }
          .map {
            DateUtils.parseDate
          }
          .map {
            _.toInstant.atOffset(ZoneOffset.UTC)
          }
          .map { _ =>
            DateTimeFormatter.RFC_1123_DATE_TIME.format(_)
          }
          .map(_.toString())
          .orNull

      case _ => null // Should return null if no value for header
    }
  }

  override def disconnect(): Unit = response.foreach { _.close() }

  override def usingProxy(): Boolean =
    sys.env.get("http.proxyHost").exists {
      _ != ""
    } || sys.env.get("https.proxyHost").exists {
      _ != ""
    }

  private def statusMessageForCode(code: Int): String =
    code match {
      case 200 => "OK"
      case 404 => "Not Found"
      case _ => "DUMMY"
    }
}
