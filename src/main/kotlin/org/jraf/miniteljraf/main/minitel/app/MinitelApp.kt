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
import org.jraf.miniteljraf.main.minitel.JrafScreen
import org.jraf.miniteljraf.main.minitel.contact.ContactScreen
import org.jraf.miniteljraf.main.minitel.main.MainScreen
import org.jraf.miniteljraf.main.minitel.projects.ProjectsScreen

class MinitelApp(private val connection: Minitel.Connection) {
  class Context {
  }

  private val context = Context()

  private lateinit var currentScreen: JrafScreen

  suspend fun start() {
    connection.screen.disableAcknowledgement()
    connection.screen.localEcho(false)
    connection.screen.scroll(true)
//    onNavigateToMain(MainScreen.StartMode.CLEAR_AND_ANIMATE_LOGO)
//    onNavigateToContact()
    onNavigateToProjects()

    connection.keyboard.collect { key ->
      currentScreen.onKeyboard(key)
    }
  }

  private suspend fun onNavigateToMain(
    mode: MainScreen.StartMode,
  ) {
    currentScreen = MainScreen(
      context = context,
      connection = connection,
      startMode = mode,
      onNavigateToContact = ::onNavigateToContact,
      onNavigateToProjects = ::onNavigateToProjects,
    )
    currentScreen.start()
  }

  private suspend fun onNavigateToContact() {
    currentScreen = ContactScreen(
      context = context,
      connection = connection,
      onNavigateToMain = {
        onNavigateToMain(MainScreen.StartMode.CLEAR_AND_KEEP_LOGO)
      },
    )
    currentScreen.start()
  }

  private suspend fun onNavigateToProjects() {
    currentScreen = ProjectsScreen(
      context = context,
      connection = connection,
      onNavigateToMain = {
        onNavigateToMain(MainScreen.StartMode.CLEAR_AND_DRAW_LOGO)
      },
    )
    currentScreen.start()
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
