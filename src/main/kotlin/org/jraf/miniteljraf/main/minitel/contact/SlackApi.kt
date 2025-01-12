/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2024-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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

package org.jraf.miniteljraf.main.minitel.contact

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import org.jraf.klibslack.client.SlackClient
import org.jraf.klibslack.client.configuration.ClientConfiguration
import org.jraf.klibslack.model.Event
import org.jraf.klibslack.model.MessageAddedEvent
import org.jraf.klibslack.model.MessageChangedEvent
import org.jraf.klibslack.model.MessageDeletedEvent
import org.jraf.klibslack.model.ReactionAddedEvent

class SlackApi {
  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val slackClient = SlackClient.newInstance(
    ClientConfiguration(
      appToken = System.getenv("slackAppToken"),
      botUserOAuthToken = System.getenv("slackBotUserOAuthToken"),
    ),
  )

  private var channelId: String? = null
  private suspend fun getChannelId(): String {
    if (channelId == null) {
      channelId = slackClient.getAllChannels().first { it.name == System.getenv("slackChannelName") }.id
    }
    return channelId!!
  }

  private var botId: String? = null
  private suspend fun getIdentityUserId(): String {
    if (botId == null) {
      botId = slackClient.getBotIdentity().id
    }
    return botId!!
  }

  private suspend fun connect(onEvent: suspend (event: Event) -> Unit) {
    val webSocketUrl = slackClient.appsConnectionsOpen()
    slackClient.openWebSocket(webSocketUrl) { event ->
      // Ignore events from other channels, and our own messages
      if (event.channel == getChannelId() && event.user != getIdentityUserId()) {
        onEvent(event)
      }
    }
  }

  private val _eventFlow: SharedFlow<Event> = callbackFlow {
    connect { event -> send(event) }
  }.shareIn(
    coroutineScope,
    replay = 0,
    started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 15_000),
  )

  val eventFlow: SharedFlow<Event> = _eventFlow

  private val Event.channel: String
    get() {
      return when (this) {
        is MessageAddedEvent -> channel
        is MessageChangedEvent -> channel
        is MessageDeletedEvent -> channel
        is ReactionAddedEvent -> channel
        else -> "(no channel)"
      }
    }

  private val Event.user: String
    get() {
      return when (this) {
        is MessageAddedEvent -> user
        is MessageChangedEvent -> user
        is MessageDeletedEvent -> user
        is ReactionAddedEvent -> user
        else -> "(no user)"
      }
    }

  suspend fun sendMessage(text: String) {
    slackClient.chatPostMessage(getChannelId(), text)
  }

  fun close() {
    coroutineScope.cancel()
  }
}

fun String.escapeEmojis(): String {
  return replace(Regex(":slightly_smiling_face:|:blush:|🙂|😊"), ":)")
    .replace(Regex(":disappointed:|:pensive:|🙁|😞|😔"), ":(")
    .replace(Regex(":smile:|:grin:|:grinning:|😁|😄"), ":D")
    .replace(Regex(":stuck_out_tongue:|😛"), ":P")
    .replace(Regex(":open_mouth:|😮"), ":O")
    .replace(Regex(":wink:|😉"), ";)")
    .replace(Regex(":cry:|😢"), ":'(")
    .replace(Regex(":joy:|😂"), ":_)")
}
