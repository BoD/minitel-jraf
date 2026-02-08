/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2026-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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

package org.jraf.miniteljraf.app.jraf

import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.jraf.klibminitel.core.Minitel
import org.jraf.miniteljraf.app.MinitelScreen
import org.jraf.miniteljraf.app.jraf.contact.ContactScreen
import org.jraf.miniteljraf.app.jraf.main.MainScreen
import org.jraf.miniteljraf.app.jraf.mastodon.MastodonScreen
import org.jraf.miniteljraf.app.jraf.playstore.PlayStoreScreen
import org.jraf.miniteljraf.app.jraf.projects.ProjectScreen
import org.jraf.miniteljraf.app.jraf.projects.ProjectsScreen
import org.jraf.miniteljraf.app.jraf.resume.ResumeScreen
import org.jraf.miniteljraf.github.GetRepositoriesQuery

class JrafMinitelApp(private val connection: Minitel.Connection) {
  class Context

  private val context = Context()

  private val screenStack = mutableListOf<MinitelScreen<*, *>>()

  private suspend fun <C, P, S : MinitelScreen<C, P>> pushScreen(screen: S, startParameters: P) {
    screenStack.lastOrNull()?.stop()
    screenStack.add(screen)
    screen.start(startParameters)
  }

  private suspend fun <C, P, S : MinitelScreen<C, P>> popScreen(startParameters: P, count: Int = 1) {
    repeat(count) {
      currentScreen.stop()
      screenStack.removeLast()
    }
    @Suppress("UNCHECKED_CAST")
    (currentScreen as S).start(startParameters)
  }

  private val currentScreen: MinitelScreen<*, *>
    get() = screenStack.last()

  suspend fun start() {
    connection.screen.scroll(true)
    onNavigateToMain(MainScreen.StartMode.CLEAR_AND_ANIMATE_LOGO)
//    onNavigateToMain(MainScreen.StartMode.CLEAR_AND_KEEP_LOGO)
//    onNavigateToContact()
//    onNavigateToProjects()
//    onNavigateToPlayStore()
//    onNavigateToMastodon()
//    onNavigateToResume()

    connection.keyboard.collect { key ->
      currentScreen.onKeyboard(key)
    }
  }

  private suspend fun onNavigateToMain(
    mode: MainScreen.StartMode,
  ) {
    pushScreen(
      MainScreen(
        context = context,
        connection = connection,
        onNavigateToContact = ::onNavigateToContact,
        onNavigateToProjects = ::onNavigateToProjects,
        onNavigateToPlayStore = ::onNavigateToPlayStore,
        onNavigateToResume = ::onNavigateToResume,
        onNavigateToMastodon = ::onNavigateToMastodon,
      ),
      mode,
    )
  }

  private suspend fun onNavigateToContact() {
    pushScreen(
      ContactScreen(
        connection = connection,
        onBack = {
          popScreen<Unit, _, _>(MainScreen.StartMode.CLEAR_AND_KEEP_LOGO)
        },
      ),
      Unit,
    )
  }

  private suspend fun onNavigateToProjects() {
    pushScreen(
      ProjectsScreen(
        context = context,
        connection = connection,
        onBack = {
          popScreen<Unit, _, _>(MainScreen.StartMode.CLEAR_AND_DRAW_LOGO)
        },
        onNavigateToProject = ::onNavigateToProject,
      ),
      false,
    )
  }

  private suspend fun onNavigateToPlayStore() {
    pushScreen(
      PlayStoreScreen(
        connection = connection,
        onBack = {
          popScreen<Unit, _, _>(MainScreen.StartMode.CLEAR_AND_DRAW_LOGO)
        },
      ),
      Unit,
    )
  }

  private suspend fun onNavigateToResume() {
    pushScreen(
      ResumeScreen(
        connection = connection,
        onBack = {
          popScreen<Unit, _, _>(MainScreen.StartMode.CLEAR_AND_DRAW_LOGO)
        },
      ),
      Unit,
    )
  }

  private suspend fun onNavigateToMastodon() {
    pushScreen(
      MastodonScreen(
        context = context,
        connection = connection,
        onBack = {
          popScreen<Unit, _, _>(MainScreen.StartMode.CLEAR_AND_DRAW_LOGO)
        },
      ),
      false,
    )
  }


  private suspend fun onNavigateToProject(repository: GetRepositoriesQuery.Node) {
    pushScreen(
      ProjectScreen(
        context = context,
        connection = connection,
        onBack = {
          popScreen<Unit, _, _>(true)
        },
      ),
      repository,
    )
  }
}

suspend fun main() {
  val minitel = Minitel(
    keyboard = System.`in`.asSource().buffered(),
    screen = System.out.asSink().buffered(),
  )
  minitel.connect {
    JrafMinitelApp(this).start()
  }
}
