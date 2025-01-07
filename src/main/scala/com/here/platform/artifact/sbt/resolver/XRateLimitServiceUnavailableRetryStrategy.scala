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

package com.here.platform.artifact.sbt.resolver

import org.apache.http.client.ServiceUnavailableRetryStrategy
import org.apache.http.protocol.HttpContext
import org.apache.http.{Header, HttpResponse, HttpStatus}
import org.apache.ivy.util.Message

import scala.util.matching.Regex
import scala.util.{Failure, Try}

/**
  * An implementation of the {@link ServiceUnavailableRetryStrategy} interface.
  * that retries {@code 408} (Request Timeout), {@code 429} (Too Many Requests),
  * {@code 500} (Server side error), {@code 502} (Bad gateway), {@code 503} (Service Unavailable) and {@code 504} (Gateway timeout)
  * responses for a fixed number of times at a interval returned in X-RateLimit-Reset or Retry-After header.
  * X-RateLimit-Reset have precedence over Retry-After.
  */
class XRateLimitServiceUnavailableRetryStrategy extends ServiceUnavailableRetryStrategy {

  val ScTooManyRequests = 429

  /**
    * Default delay between retries. Applied only if X-RateLimit-Reset and Retry-After headers are absent in response
    */
  val DefaultRetryIntervalMs = 5000

  /**
    * Maximum retries count
    */
  val MaxRetries = 5

  /**
    * The response HTTP header indicates how long the user agent should wait before making a follow-up request.
    * Custom header for Artifact Service. This should be same as the Retry-After header
    */
  val XRateLimitResetHeader = "X-RateLimit-Reset"

  /**
    * The response HTTP header indicates how long the user agent should wait before making a follow-up request.
    * Standard header for Artifact Service
    */
  val RetryAfterHeader = "Retry-After"

  val DigitPattern: Regex = "^\\d+$".r

  val currentResponse = new ThreadLocal[HttpResponse]

  override def retryRequest(response: HttpResponse, executionCount: Int, context: HttpContext): Boolean = {
    currentResponse.set(response)
    val statusCode = response.getStatusLine.getStatusCode
    val retryableStatusCode = statusCode == HttpStatus.SC_REQUEST_TIMEOUT ||
      statusCode == ScTooManyRequests ||
      statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR ||
      statusCode == HttpStatus.SC_BAD_GATEWAY ||
      statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE ||
      statusCode == HttpStatus.SC_GATEWAY_TIMEOUT
    executionCount <= MaxRetries && retryableStatusCode
  }

  override def getRetryInterval: Long = {
    val evaluatedRetryInterval = for {
      httpResponse <- Option(currentResponse.get)
      waitHeader <- getWaitHeader(httpResponse)
      parsedInterval <- parseLongValueFromHeader(waitHeader)
      parsedInterval <- logRetry(httpResponse, parsedInterval)
    } yield parsedInterval
    evaluatedRetryInterval.getOrElse(DefaultRetryIntervalMs)
  }

  def getWaitHeader(response: HttpResponse): Option[Header] = Try(
    Option(response.getFirstHeader(XRateLimitResetHeader)).orElse(Option(response.getFirstHeader(RetryAfterHeader)))
  ).recoverWith {
    case e => Message.warn("Unexpected exception occurred. Fallback to standard retry logic"); Failure(e)
  }.toOption.flatten

  def parseLongValueFromHeader(header: Header): Option[Long] = Try {
    Option(header.getValue).flatMap(DigitPattern.findFirstIn(_)).map(_.toLong * 1000)
  }.recoverWith {
    case e => Message.warn(s"Could not parse value [${header.getValue}] from header [${header.getName}]"); Failure(e)
  }.toOption.flatten

  def logRetry(httpResponse: HttpResponse, retryInterval: Long): Option[Long] = {
    Message.info(s"Request is failed with code ${httpResponse.getStatusLine.getStatusCode}. Retrying in ${retryInterval} milliseconds")
    Some(retryInterval)
  }

}
