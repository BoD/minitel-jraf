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

package org.jraf.miniteljraf.main.minitel.app

import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.jraf.klibminitel.core.Minitel
import org.jraf.miniteljraf.github.GetRepositoriesQuery
import org.jraf.miniteljraf.main.minitel.JrafScreen
import org.jraf.miniteljraf.main.minitel.contact.ContactScreen
import org.jraf.miniteljraf.main.minitel.main.MainScreen
import org.jraf.miniteljraf.main.minitel.projects.MastodonScreen
import org.jraf.miniteljraf.main.minitel.projects.ProjectScreen
import org.jraf.miniteljraf.main.minitel.projects.ProjectsScreen

class MinitelApp(private val connection: Minitel.Connection) {
  class Context {
  }

  private val context = Context()

  private val screenStack = mutableListOf<JrafScreen<*>>()

  private suspend fun <P, S : JrafScreen<P>> pushScreen(screen: S, startParameters: P) {
    screenStack.lastOrNull()?.stop()
    screenStack.add(screen)
    screen.start(startParameters)
  }

  private suspend fun <P, S : JrafScreen<P>> popScreen(startParameters: P, count: Int = 1) {
    repeat(count) {
      currentScreen.stop()
      screenStack.removeLast()
    }
    @Suppress("UNCHECKED_CAST")
    (currentScreen as S).start(startParameters)
  }

  private val currentScreen: JrafScreen<*>
    get() = screenStack.last()

  suspend fun start() {
    connection.screen.disableAcknowledgement()
    connection.screen.localEcho(false)
    connection.screen.scroll(true)
    onNavigateToMain(MainScreen.StartMode.CLEAR_AND_ANIMATE_LOGO)
//    onNavigateToContact()
//    onNavigateToProjects()
//    onNavigateToMastodon()

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
        onNavigateToMastodon = ::onNavigateToMastodon,
      ),
      mode,
    )
  }

  private suspend fun onNavigateToContact() {
    pushScreen(
      ContactScreen(
        context = context,
        connection = connection,
        onBack = {
          popScreen(MainScreen.StartMode.CLEAR_AND_KEEP_LOGO)
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
          popScreen(MainScreen.StartMode.CLEAR_AND_DRAW_LOGO)
        },
        onNavigateToProject = ::onNavigateToProject,
      ),
      false,
    )
  }

  private suspend fun onNavigateToMastodon() {
    pushScreen(
      MastodonScreen(
        context = context,
        connection = connection,
        onBack = {
          popScreen(MainScreen.StartMode.CLEAR_AND_DRAW_LOGO)
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
          popScreen(true)
        },
      ),
      repository,
    )
  }
}

suspend fun main(av: Array<String>) {
  val minitel = Minitel(
    keyboard = System.`in`.asSource().buffered(),
    screen = System.out.asSink().buffered(),
  )
  minitel.connect {
    MinitelApp(this).start()
  }
}
