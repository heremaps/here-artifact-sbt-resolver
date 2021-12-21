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

import java.net.{URL, URLStreamHandler, URLStreamHandlerFactory}

import org.apache.ivy.util.url.{URLHandlerDispatcher, URLHandlerRegistry}
import sbt.Keys._
import sbt._

object ArtifactResolverPlugin extends AutoPlugin {

  // This plugin will load automatically
  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[_]] = Seq(
    onLoad in Global :=  { state: State =>
      def info: String => Unit = state.log.info(_)

      def debug: String => Unit = state.log.debug(_)

      // We need 'here+' URLs to work without throwing a java.net.MalformedURLException
      // which means installing a dummy URLStreamHandler.  We only install the handler
      // if it's not already installed (since a second call to URL.setURLStreamHandlerFactory
      // will fail).
      try {
        new URL("here+artifact-service://example.com")
        debug("here+artifact-service:// URLStreamHandler is already installed")
      } catch {
        // This means we haven't installed the handler, so install it
        case _: java.net.MalformedURLException =>
          info(
            "Installing the here+artifact-service// URLStreamHandler via java.net.URL.setURLStreamHandlerFactory")
          URL.setURLStreamHandlerFactory(HereURLStreamHandlerFactory)
      }

      //
      // This sets up the Ivy URLHandler for 'here+' URLs
      //
      val dispatcher: URLHandlerDispatcher = URLHandlerRegistry.getDefault match {
        // If the default is already a URLHandlerDispatcher then just use that
        case dispatcher: URLHandlerDispatcher =>
          debug("Using the existing Ivy URLHandlerDispatcher to handle 'here+' URLs")
          dispatcher
        // Otherwise create a new URLHandlerDispatcher
        case default =>
          info("Creating a new Ivy URLHandlerDispatcher to handle 'here+' URLs")
          val dispatcher: URLHandlerDispatcher = new URLHandlerDispatcher()
          dispatcher.setDefault(default)
          URLHandlerRegistry.setDefault(dispatcher)
          dispatcher
      }

      // Register (or replace) the 'here+' handler
      dispatcher.setDownloader("here+artifact-service", new ArtifactURLHandler)

      state
    } andThen (onLoad in Global).value
  )
}

private object HereURLStreamHandlerFactory extends URLStreamHandlerFactory {
  def createURLStreamHandler(protocol: String): URLStreamHandler = protocol match {
    case "here+artifact-service" => new com.here.platform.artifact.sbt.resolver.connection.Handler
    case _ => null
  }
}
