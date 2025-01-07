/*
 * Copyright (C) 2018-2025 HERE Europe B.V.
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

import org.apache.http.protocol.HttpContext
import org.apache.http.{Header, HttpResponse, StatusLine}
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

class XRateLimitServiceUnavailableRetryStrategyTest extends AnyFunSuite with OneInstancePerTest with MockFactory {

  private val httpResponse = mock[HttpResponse]

  private val httpContext = mock[HttpContext]

  private val strategy = new XRateLimitServiceUnavailableRetryStrategy

  private val statusLineMock = mock[StatusLine]
  (statusLineMock.getStatusCode _).stubs().returning(429)
  (httpResponse.getStatusLine _).stubs().returning(statusLineMock)

  test("test retry for 429 response code") {
    val result = strategy.retryRequest(httpResponse, 1, httpContext)
    result should equal (true)
  }

  test("test retry interval is used from XRateLimitReset header") {
    val headerMock = mock[Header]
    (httpResponse.getFirstHeader _).expects(strategy.XRateLimitResetHeader).returning(headerMock)
    (headerMock.getValue _).expects().returning("99")
    strategy.retryRequest(httpResponse, 1, httpContext)
    val result = strategy.getRetryInterval
    result should equal (99000)
  }

  test("test retry interval is used from XRateLimitReset header when RetryAfter header present") {
    val headerMockXrateLimit = mock[Header]
    (httpResponse.getFirstHeader _).expects(strategy.XRateLimitResetHeader).returning(headerMockXrateLimit)
    (headerMockXrateLimit.getValue _).expects().returning("99")

    val headerRetryAfter = mock[Header]
    (httpResponse.getFirstHeader _).stubs(strategy.RetryAfterHeader).returning(headerRetryAfter)
    (headerRetryAfter.getValue _).stubs().returning("1")

    strategy.retryRequest(httpResponse, 1, httpContext)
    val result = strategy.getRetryInterval
    result should equal (99000)
  }

  test("test retry interval is used from RetryAfter header") {
    val headerRetryAfter = mock[Header]
    (httpResponse.getFirstHeader _).stubs(strategy.RetryAfterHeader).returning(headerRetryAfter)
    (headerRetryAfter.getValue _).stubs().returning("1")
    (httpResponse.getFirstHeader _).stubs(strategy.XRateLimitResetHeader).returning(null)

    strategy.retryRequest(httpResponse, 1, httpContext)
    val result = strategy.getRetryInterval
    result should equal (1000)
  }

  test("test default retry interval is used when XRateLimitReset header have non numeric value") {
    val headerMockXrateLimit = mock[Header]
    (httpResponse.getFirstHeader _).expects(strategy.XRateLimitResetHeader).returning(headerMockXrateLimit)
    (headerMockXrateLimit.getValue _).expects().returning("asd")
    strategy.retryRequest(httpResponse, 1, httpContext)
    val result = strategy.getRetryInterval
    result should equal (5000)
  }

  test("test default retry interval is used when XRateLimitReset header have non numeric value with numeric prefix") {
    val headerMockXrateLimit = mock[Header]
    (httpResponse.getFirstHeader _).expects(strategy.XRateLimitResetHeader).returning(headerMockXrateLimit)
    (headerMockXrateLimit.getValue _).expects().returning("123asd")
    strategy.retryRequest(httpResponse, 1, httpContext)
    val result = strategy.getRetryInterval
    result should equal (5000)
  }

  test("test default retry interval is used when exception occurred") {
    val headerMockXrateLimit = mock[Header]
    (httpResponse.getFirstHeader _).stubs(strategy.XRateLimitResetHeader).throwing(new RuntimeException("Stubbed exception"))
    strategy.retryRequest(httpResponse, 1, httpContext)
    val result = strategy.getRetryInterval
    result should equal (5000)
  }

}