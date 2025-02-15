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

package org.jraf.miniteljraf.main.minitel.playstore

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.main.minitel.ParameterlessJrafScreen
import org.jraf.miniteljraf.main.minitel.app.MinitelApp

class PlayStoreScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onBack: suspend () -> Unit,
) : ParameterlessJrafScreen(context, connection) {
  override suspend fun start() {
    connection.screen.drawScreen()
  }

  private suspend fun Minitel.Screen.drawScreen() {
    clearScreenAndHome()
    drawHeader()
    drawFooter()
    drawText()
  }

  private suspend fun Minitel.Screen.drawHeader() {
    moveCursor(0, 1)
    characterSize(CharacterSize.TALL)
    colorForeground(5)
    colorBackground(1)
    print(" 3615 JRAF |")
    colorForeground(7)
    print(" Play Store")
    clearEndOfLine()
    moveCursor(0, 2)
    colorForeground(5)
    repeatCharacter('~', SCREEN_WIDTH_NORMAL)
  }

  private suspend fun Minitel.Screen.drawText() {
    moveCursor(0, 3)
    colorForeground(5)
    print(
      """
        Over the years I've worked on a few
        little Android apps.
        
        I've published some of them on the
        Play Store.
        And some of them are even still there!
        
        You can see them all at:
      """.trimIndent(),
    )
    print("\n\n")
    colorForeground(7)
    underline(true)
    print("\u0019\u002e https://JRAF.org/playstore")
  }

  private suspend fun Minitel.Screen.drawFooter() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 2)

    color(1, 5)
    print(" Back to main screen")
    underline(true)
    print(" ")
    inverse(true)
    print(" Sommaire ")
    underline(false)
    inverse(false)
    print(" ")
    clearEndOfLine()
    print("\n")

    color(1, 5)
    print(" ")
    clearEndOfLine()
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (e) {
      is Minitel.KeyboardEvent.FunctionKeyEvent -> when (e.functionKey) {
        FunctionKey.SOMMAIRE, FunctionKey.RETOUR -> onBack()
        else -> {}
      }

      else -> {}
    }
  }
}
