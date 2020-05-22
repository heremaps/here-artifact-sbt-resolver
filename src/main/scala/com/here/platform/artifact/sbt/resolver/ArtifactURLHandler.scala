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

package com.here.platform.artifact.sbt.resolver

import java.io.{File, InputStream}
import java.net.URL
import java.util.Date

import com.here.platform.artifact.sbt.resolver.ArtifactURLHandler.ArtifactURLInfo
import com.here.platform.artifact.sbt.resolver.error.ArtifactNotFoundException
import com.here.platform.artifact.sbt.resolver.utils.HttpUtils._
import com.here.platform.artifact.sbt.resolver.utils.RegisterResponse
import org.apache.http.client.methods.{HttpGet, HttpHead, HttpPut}
import org.apache.http.entity.FileEntity
import org.apache.ivy.util.{CopyProgressEvent, CopyProgressListener, Message}
import org.apache.ivy.util.url.URLHandler
import org.apache.ivy.util.url.URLHandler.UNAVAILABLE

object ArtifactURLHandler {

  private class ArtifactURLInfo(available: Boolean, contentLength: Long, lastModified: Long)
      extends URLHandler.URLInfo(available, contentLength, lastModified)
}

/**
  * This implements the Ivy URLHandler
  */
final class ArtifactURLHandler extends URLHandler {

  override def isReachable(url: URL): Boolean = getURLInfo(url).isReachable
  override def isReachable(url: URL, timeout: Int): Boolean = getURLInfo(url, timeout).isReachable
  override def getContentLength(url: URL): Long = getURLInfo(url).getContentLength
  override def getContentLength(url: URL, timeout: Int): Long =
    getURLInfo(url, timeout).getContentLength
  override def getLastModified(url: URL): Long = getURLInfo(url).getLastModified
  override def getLastModified(url: URL, timeout: Int): Long =
    getURLInfo(url, timeout).getLastModified
  override def getURLInfo(url: URL): URLHandler.URLInfo = getURLInfo(url, 0)

  override def getURLInfo(url: URL, timeout: Int): URLHandler.URLInfo = {
    val artifact = toArtifact(url.toString)
    try {
      val groupHrnPrefix = registerExists(artifact.groupId, artifact.artifactId).groupHrnPrefix
      val newUrl = rewriteUrl(groupHrnPrefix, artifact)

      val httpHead = new HttpHead(newUrl)
      val response = executeRequest(httpHead)

      val contentLength = Option(response.getFirstHeader("Content-Length"))
        .map(_.getValue.toLong)
        .orElse(Option(0.toLong))
        .get

      val lastModified = Option(response.getFirstHeader("Last-Modified"))
        .map(date => org.apache.http.client.utils.DateUtils.parseDate(date.getValue))
        .orElse(Option(new Date()))
        .get

      new ArtifactURLInfo(true, contentLength, lastModified.getTime);
    } catch {
      case _: Exception => UNAVAILABLE
    }
  }

  override def openStream(url: URL): InputStream = {
    val artifact = toArtifact(url.toString)
    val groupHrnPrefix = registerExists(artifact.groupId, artifact.artifactId).groupHrnPrefix
    val newUrl = rewriteUrl(groupHrnPrefix, artifact)
    val httpGet = new HttpGet(newUrl)
    val response = executeRequest(httpGet)

    assertStatusCode(
      response,
      s"Open of $newUrl stream has failed with ${response.getStatusLine.getStatusCode}. Reason: ${response.getStatusLine.getReasonPhrase}"
    )

    response.getEntity.getContent
  }

  override def download(src: URL, dest: File, l: CopyProgressListener): Unit = {
    val event: CopyProgressEvent = new CopyProgressEvent()
    if (null != l) l.start(event)
    val artifact = toArtifact(src.toString)
    val groupHrnPrefix = registerExists(artifact.groupId, artifact.artifactId).groupHrnPrefix
    val newUrl = rewriteUrl(groupHrnPrefix, artifact)
    val httpGet = new HttpGet(newUrl)
    val response = executeRequest(httpGet)

    assertStatusCode(
      response,
      s"Download of $newUrl to ${dest.getAbsoluteFile} has failed with ${response.getStatusLine.getStatusCode}. Reason: ${response.getStatusLine.getReasonPhrase}"
    )

    writeBytes(response.getEntity.getContent, dest)

    val lastModified = Option(response.getFirstHeader("Last-Modified"))
      .map { date =>
        org.apache.http.client.utils.DateUtils.parseDate(date.getValue)
      }
      .orElse(Option(new Date()))
      .get
      .getTime

    dest.setLastModified(lastModified)

    if (null != l) l.end(event)
  }

  override def upload(src: File, dest: URL, l: CopyProgressListener): Unit = {
    Message.info(s"Uploading ${dest}")
    val artifact = toArtifact(dest.toString)
    val registerResponse = registerAndUpload(artifact.groupId, artifact.artifactId)
    val event: CopyProgressEvent = new CopyProgressEvent()
    if (null != l) l.start(event)

    val newUrl = rewriteUrl(registerResponse.groupHrnPrefix, artifact)
    val httpPut = new HttpPut(newUrl)
    httpPut.setEntity(new FileEntity(src))
    val response = executeRequest(httpPut)

    assertStatusCode(
      response,
      s"Upload of ${src.getAbsoluteFile} to $newUrl has failed with ${response.getStatusLine.getStatusCode}. Reason: ${response.getStatusLine.getReasonPhrase}"
    )
    if (null != l) l.end(event)
  }

  override def setRequestMethod(requestMethod: Int): Unit =
    Message.debug(s"setRequestMethod($requestMethod)")

  private def registerAndUpload(groupId: String, artifactId: String): RegisterResponse =
    try {
      registerExists(groupId, artifactId)
    } catch {
      case _: ArtifactNotFoundException => registerArtifact(groupId, artifactId)
    }

}
