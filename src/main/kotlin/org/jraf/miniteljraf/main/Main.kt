/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.miniteljraf.main

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.core.readFully
import io.ktor.utils.io.core.writeFully
import io.ktor.websocket.Frame
import kotlinx.coroutines.runBlocking
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import org.jraf.klibminitel.core.Minitel
import org.jraf.miniteljraf.main.minitel.app.MinitelApp
import org.slf4j.simple.SimpleLogger

private const val DEFAULT_PORT = 8080

private const val ENV_PORT = "PORT"

private fun initLogs() {
  // This must be done before any logger is initialized
  System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out")
  System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "trace")
  System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true")
  System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss")
}

fun main() {
  initLogs()
  val listenPort = System.getenv(ENV_PORT)?.toInt() ?: DEFAULT_PORT
  embeddedServer(Netty, listenPort) {
    install(DefaultHeaders)
    install(ContentNegotiation) {
      json(
        Json {
          prettyPrint = true
        },
      )
    }
    install(StatusPages)
    install(WebSockets)

    routing {
      webSocket("/ws") {
        val keyboardSource = object : RawSource {
          private val buffer = Buffer()

          override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
            if (buffer.exhausted()) {
              val frame = runBlocking { incoming.receive() }
              when (frame) {
                is Frame.Text, is Frame.Binary -> {
                  val data = frame.data
                  buffer.writeFully(data)
                }

                else -> {}
              }
            }
            return buffer.readAtMostTo(sink, byteCount)
          }

          override fun close() {}
        }

        val screenSink = object : RawSink {
          override fun write(source: Buffer, byteCount: Long) {
            val data = ByteArray(byteCount.toInt())
            source.readFully(data)
            runBlocking { outgoing.send(Frame.Text(true, data)) }
          }

          override fun flush() {
          }

          override fun close() {}
        }

        val minitel = Minitel(
          keyboard = keyboardSource.buffered(),
          screen = screenSink.buffered(),
        )

        minitel.connect {
          MinitelApp(this).start()
        }
      }
    }
  }.start(wait = true)
}
