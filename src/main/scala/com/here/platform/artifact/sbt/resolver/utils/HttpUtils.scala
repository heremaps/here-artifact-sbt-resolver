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

import com.here.platform.artifact.sbt.resolver.XRateLimitServiceUnavailableRetryStrategy

import java.io.{File, IOException, InputStream}
import java.net.HttpURLConnection._
import java.nio.file.{Files, StandardCopyOption}
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpPut, HttpUriRequest}
import org.apache.ivy.util.Message
import com.here.platform.artifact.sbt.resolver.auth.HereAuth
import com.here.platform.artifact.sbt.resolver.error.{ArtifactNotFoundException, RegisterException}
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.util.EntityUtils
import org.apache.http.impl.client.HttpClientBuilder


import scala.util.parsing.json.JSON

case class RegisterResponse(groupId: String,
                            artifactId: String,
                            hrnPrefix: String,
                            groupHrnPrefix: String)

case class Artifact(groupId: String, artifactId: String, version: String, file: String)

object HttpUtils {

  private val ARTIFACT_SERVICE_URL_PLACEHOLDER = "here+artifact-service://artifact-service/"

  private[artifact] def registerExists(groupId: String,
                                       artifactId: String): RegisterResponse = {
    require(groupId.nonEmpty, "groupId mustn't be empty!")
    require(artifactId.nonEmpty, "artifactId mustn't be empty!")

    val hereTokenEndpointUrl = HereAuth.getTokenEndpointUrl
    val artifactServiceUrl =
      ArtifactPropertiesResolver.resolveArtifactServiceUrl(hereTokenEndpointUrl)
    val registerUrl = s"$artifactServiceUrl/register/$groupId/$artifactId"
    val contentType = "application/json"

    val httpRequest = new HttpGet(registerUrl)

    val response = executeRequest(httpRequest, Some(contentType))

    val statusCode = response.getStatusLine.getStatusCode
    val content = EntityUtils.toString(response.getEntity)
    if (statusCode == HTTP_OK) validatedAndParseRegister(content)
    else throw ArtifactNotFoundException(if (content.nonEmpty) content else response.getStatusLine.getReasonPhrase)
  }

  private[artifact] def registerArtifact(groupId: String, artifactId: String): RegisterResponse = {
    require(groupId.nonEmpty, "groupId mustn't be empty!")
    require(artifactId.nonEmpty, "artifactId mustn't be empty!")

    val hereTokenEndpointUrl = HereAuth.getTokenEndpointUrl
    val artifactServiceUrl =
      ArtifactPropertiesResolver.resolveArtifactServiceUrl(hereTokenEndpointUrl)
    val registerUrl = s"$artifactServiceUrl/register/$groupId/$artifactId"
    val httpRequest = new HttpPut(registerUrl)
    val contentType = "application/json"
    HereAuth.getUser.foreach(user => {
      val body = s"""{ "userId": "$user" }"""
      val entity = new ByteArrayEntity(body.getBytes("UTF-8"))
      httpRequest.setEntity(entity)
    })

    val response = executeRequest(httpRequest, Some(contentType))

    val statusCode = response.getStatusLine.getStatusCode
    val content = EntityUtils.toString(response.getEntity)
    statusCode match {
      case HTTP_OK | HTTP_CREATED => validatedAndParseRegister(content)
      case _ =>
        throw RegisterException(
          if (content.nonEmpty) content
          else response.getStatusLine.getReasonPhrase)
    }
  }

  private def validatedAndParseRegister(content: String) = {
    val result = JSON.parseFull(content)
    result match {
      case Some(map: Map[String, Any]) =>
        RegisterResponse(
          map("groupId").toString,
          map("artifactId").toString,
          map("hrnPrefix").toString,
          map("groupHrnPrefix").toString
        )
      case None => throw new IllegalArgumentException("Parsing failed")
      case other => throw new IllegalArgumentException(s"Unknown data structure: $other")
    }
  }

  private[artifact] def rewriteUrl(groupHrnPrefix: String, artifact: Artifact): String = {
    // resolve artifactServiceUrl by here token endpoint url.
    val hereTokenEndpointUrl = HereAuth.getTokenEndpointUrl
    val artifactServiceUrl =
      ArtifactPropertiesResolver.resolveArtifactServiceUrl(hereTokenEndpointUrl)

    "%s/%s:%s:%s/%s".format(artifactServiceUrl,
                            groupHrnPrefix,
                            artifact.artifactId,
                            artifact.version,
                            artifact.file)
  }

  private[artifact] def toArtifact(url: String): Artifact = {
    val path = url match {
      case url if url.startsWith(ARTIFACT_SERVICE_URL_PLACEHOLDER) =>
        url.substring(ARTIFACT_SERVICE_URL_PLACEHOLDER.length)
      case _ => throw new IllegalArgumentException("Invalid placeholder url for artifact service")
    }
    val parts = path.split("/")

    val groupId = parts.take(parts.length - 3).mkString(".")
    val artifactId = parts(parts.length - 3)
    val version = parts(parts.length - 2)
    val file = parts(parts.length - 1)
    Artifact(groupId, artifactId, version, file)
  }

  private[artifact] def setBearer(httpRequest: HttpUriRequest): Unit = {
    def authorization = HereAuth.getToken
    Message.debug(s"Obtained bearer token: [$authorization]")
    httpRequest.setHeader("Authorization", s"Bearer $authorization")
  }

  private[artifact] def executeRequest(
      httpRequest: HttpUriRequest,
      contentType: Option[String] = None): CloseableHttpResponse = {

    setBearer(httpRequest)
    httpRequest.addHeader("Cache-control", "no-cache")
    httpRequest.addHeader("Cache-store", "no-store")
    httpRequest.addHeader("Pragma", "no-cache")
    httpRequest.addHeader("Expires", "0")
    httpRequest.addHeader("Accept-Encoding", "gzip")
    contentType.foreach(httpRequest.addHeader("Content-Type", _))

    val client = HttpClientBuilder.create
      .setServiceUnavailableRetryStrategy(new XRateLimitServiceUnavailableRetryStrategy)
      .build
    val response = client.execute(httpRequest)
    response
  }

  private[artifact] def writeBytes(data: InputStream, file: File) =
    Files.copy(data, file.toPath, StandardCopyOption.REPLACE_EXISTING)

  private[artifact] def assertStatusCode(response: CloseableHttpResponse,
                                         failMessage: String): Unit = {
    val statusCode = response.getStatusLine.getStatusCode
    if (statusCode != 200 && statusCode != 201) { throw new IOException(failMessage) }
  }

}
