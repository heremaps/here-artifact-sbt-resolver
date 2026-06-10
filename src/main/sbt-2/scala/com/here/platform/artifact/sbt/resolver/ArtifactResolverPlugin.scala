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

import java.net.{URL, URLStreamHandler, URLStreamHandlerFactory}

import org.apache.ivy.util.url.{URLHandlerDispatcher, URLHandlerRegistry}
import sbt.Keys._
import sbt._

object ArtifactResolverPlugin extends AutoPlugin {

  // This plugin will load automatically
  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[?]] = Seq(
    Global / onLoad := {
      val previousOnLoad = (Global / onLoad).value

      (state: State) => {
        val log = state.globalLogging.full
        val debug: String => Unit = log.debug(_)
        val info: String => Unit = log.info(_)

        try {
          new java.net.URI("here+artifact-service://example.com").toURL
          debug("here+artifact-service:// URLStreamHandler is already installed")
        } catch {
          case _: java.net.MalformedURLException =>
            info(
              "Installing the here+artifact-service// URLStreamHandler via java.net.URL.setURLStreamHandlerFactory")
            URL.setURLStreamHandlerFactory(HereURLStreamHandlerFactory)
        }

        val dispatcher: URLHandlerDispatcher = URLHandlerRegistry.getDefault match {
          case dispatcher: URLHandlerDispatcher =>
            debug("Using the existing Ivy URLHandlerDispatcher to handle 'here+' URLs")
            dispatcher

          case default =>
            info("Creating a new Ivy URLHandlerDispatcher to handle 'here+' URLs")
            val dispatcher = new URLHandlerDispatcher()
            dispatcher.setDefault(default)
            URLHandlerRegistry.setDefault(dispatcher)
            dispatcher
        }

        dispatcher.setDownloader("here+artifact-service", new ArtifactURLHandler)

        previousOnLoad(state)
      }
    }
  )
}

private object HereURLStreamHandlerFactory extends URLStreamHandlerFactory {
  def createURLStreamHandler(protocol: String): URLStreamHandler = protocol match {
    case "here+artifact-service" => new com.here.platform.artifact.sbt.resolver.connection.Handler
    case _ => null
  }
}
