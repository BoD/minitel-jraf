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

package org.jraf.miniteljraf.main.minitel.projects

import com.apollographql.apollo.ApolloClient
import org.jraf.miniteljraf.github.GetRepositoriesQuery

class GitHubApi {
  private val apolloClient = ApolloClient.Builder()
    .serverUrl("https://api.github.com/graphql")
    .addHttpHeader("Authorization", "Bearer ${System.getenv("githubOauthKey")}")
    .build()

  private var repositories: List<GetRepositoriesQuery.Node>? = null

  suspend fun getRepositories(after: GetRepositoriesQuery.Node?): List<GetRepositoriesQuery.Node> {
    if (repositories == null) {
      val response = apolloClient.query(GetRepositoriesQuery("BoD")).execute()
      repositories = response.data?.user?.repositories?.nodes.orEmpty().filterNotNull()
    }
    if (after == null) {
      return repositories!!
    }
    return repositories!!.dropWhile { it != after }.drop(1)
  }
}