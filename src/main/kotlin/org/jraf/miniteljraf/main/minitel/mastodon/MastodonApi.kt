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

package org.jraf.miniteljraf.main.minitel.mastodon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jraf.miniteljraf.util.escapeEmoji
import org.jsoup.Jsoup

class MastodonApi {
  private val okHttpClient = OkHttpClient.Builder()
    .build()

  private val json = Json {
    ignoreUnknownKeys = true
  }

  private var posts: List<Post>? = null

  suspend fun getPosts(after: Post?): List<Post> {
    if (posts == null) {
      posts = withContext(Dispatchers.IO) {
        val apiResponse = okHttpClient.newCall(
          Request.Builder()
            .url("https://mastodon.social/api/v1/accounts/37127/statuses")
            .build(),
        ).execute()
        val bodyStr = apiResponse.body.string()
        json.decodeFromString<List<MastodonStatus>>(bodyStr).map { it.toPost() }
      }
    }
    if (after == null) {
      return posts!!
    }
    return posts!!.dropWhile { it != after }.drop(1)
  }

  @Serializable
  @Suppress("PropertyName")
  private data class MastodonStatus(
    val id: String,
    val created_at: String,
    val uri: String,
    val content: String,
    val reblog: MastodonStatus?,
    val account: MastodonAccount,
  )

  @Serializable
  private data class MastodonAccount(
    val acct: String,
  )

  data class Post(
    val createdAt: String,
    val text: String,
  )

  // "2024-11-07T10:19:13.390Z" -> "November 7, 2024, 10:19"
  private fun String.formatDate(): String {
    val instant = java.time.Instant.parse(this)
    val zonedDateTime = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
    return zonedDateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm"))
  }

  private fun MastodonStatus.toPost(): Post {
    return Post(
      createdAt = created_at.formatDate(),
      text = if (reblog != null) {
        "Repost from ${reblog.account.acct}: ${Jsoup.parse(reblog.content).text()}"
      } else {
        Jsoup.parse(content).text()
      }.escapeEmoji(),
    )
  }
}
