/*
 * Copyright (C) 2019-2022 HERE Europe B.V.
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

import org.apache.http.client.ServiceUnavailableRetryStrategy
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpResponse, HttpStatus}
import org.apache.ivy.util.Message

import java.util.regex.Pattern

/**
  * An implementation of the {@link ServiceUnavailableRetryStrategy} interface.
  * that retries {@code 408} (Request Timeout), {@code 429} (Too Many Requests),
  * {@code 500} (Server side error), {@code 502} (Bad gateway), {@code 503} (Service Unavailable) and {@code 504} (Gateway timeout)
  * responses for a fixed number of times at a interval returned in X-RateLimit-Reset or Retry-After header.
  * X-RateLimit-Reset have precedence over Retry-After.
  */
class XRateLimitServiceUnavailableRetryStrategy extends ServiceUnavailableRetryStrategy {

  private val SC_TOO_MANY_REQUESTS = 429

  /**
    * Default delay between retries. Applied only if X-RateLimit-Reset and Retry-After headers are absent in response
    */
  private val DEFAULT_RETRY_INTERVAL_MS = 5000

  /**
    * Maximum retries count
    */
  private val MAX_RETRIES = 5

  /**
    * The response HTTP header indicates how long the user agent should wait before making a follow-up request.
    * Custom header for Artifact Service. This should be same as the Retry-After header
    */
  final val X_RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset"

  /**
    * The response HTTP header indicates how long the user agent should wait before making a follow-up request.
    * Standard header for Artifact Service
    */
  final val RETRY_AFTER_HEADER = "Retry-After"

  private val DIGIT_PATTERN = Pattern.compile("\\d+")

  private val currentResponse = new ThreadLocal[HttpResponse]

  override def retryRequest(response: HttpResponse, executionCount: Int, context: HttpContext): Boolean = {
    currentResponse.set(response)
    val statusCode = response.getStatusLine.getStatusCode
    val retryableStatusCode = statusCode == HttpStatus.SC_REQUEST_TIMEOUT ||
      statusCode == SC_TOO_MANY_REQUESTS ||
      statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR ||
      statusCode == HttpStatus.SC_BAD_GATEWAY ||
      statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE ||
      statusCode == HttpStatus.SC_GATEWAY_TIMEOUT
    executionCount <= MAX_RETRIES && retryableStatusCode
  }

  override def getRetryInterval: Long = {
    val httpResponse = currentResponse.get
    if (httpResponse != null) try {
      val waitHeader = if (httpResponse.containsHeader(X_RATE_LIMIT_RESET_HEADER))
        httpResponse.getFirstHeader(X_RATE_LIMIT_RESET_HEADER)
      else
        httpResponse.getFirstHeader(RETRY_AFTER_HEADER)
      if (waitHeader != null) {
        val value = waitHeader.getValue
        if (value != null && DIGIT_PATTERN.matcher(value).matches) {
          Message.info(s"Request is failed with code ${httpResponse.getStatusLine.getStatusCode}. Retrying in ${value} seconds")
          return value.toLong * 1000
        } else
          Message.warn(s"Header ${waitHeader.getName} have value ${waitHeader.getValue} but numeric value expected")
      }
    } catch {
      case e: Exception =>
        Message.warn(s"Unexpected exception occurred. Fallback to standard retry logic. ${e}")
    }
    DEFAULT_RETRY_INTERVAL_MS
  }

}
