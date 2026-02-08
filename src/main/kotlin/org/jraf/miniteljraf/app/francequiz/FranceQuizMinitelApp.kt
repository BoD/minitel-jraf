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

package org.jraf.miniteljraf.app.francequiz

import kotlinx.coroutines.delay
import org.jraf.klibminitel.core.Minitel
import org.jraf.miniteljraf.Resources
import kotlin.time.Duration.Companion.seconds

class FranceQuizMinitelApp(private val connection: Minitel.Connection) {
  suspend fun start() {
    splashScreen()

    val screen = FranceQuizScreen(connection)
    screen.start()
    connection.keyboard.collect { key ->
      screen.onKeyboard(key)
    }

//    with(connection.screen) {
//      clearScreenAndHome()
//      colorForeground(1)
//      print(" Hello world! Hello world! Hello world! Hello world! Hello world! Hello world!")
//      moveCursor(0,0)
////      colorBackground(7)
//      inverse(true)
//      print(" H")
//    }
  }

  private suspend fun splashScreen() {
    with(connection.screen) {
      clearScreenAndHome()
      graphicsMode(true)
      raw(Resources.franceQuizSplashScreen)
    }
    delay(20.seconds)
  }
}
