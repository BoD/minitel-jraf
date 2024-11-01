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

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.miniteljraf.main.minitel.JrafScreen
import org.jraf.miniteljraf.main.minitel.Resources
import org.jraf.miniteljraf.main.minitel.app.MinitelApp

class ContactScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onNavigateToMain: suspend () -> Unit,
) : JrafScreen(context, connection) {
  override suspend fun start() {
    connection.screen.drawScreen()
  }

  private suspend fun Minitel.Screen.drawScreen() {
    drawText()
    drawFooter()
  }

  private suspend fun Minitel.Screen.drawText() {
    moveCursor(0, Resources.logoHeight)
    clearBottomOfScreen()
    moveCursor(3, Resources.logoHeight + 2)
    colorForeground(4)
    print("You can contact me at")
    colorForeground(7)
    underline(true)
    characterSize(CharacterSize.TALL)
    print(" BoD@JRAF.org")
//    characterSize(CharacterSize.NORMAL)
  }

  private suspend fun Minitel.Screen.drawFooter() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 2)
    colorForeground(5)
    colorBackground(1)
    print(" Press")
    underline(true)
    print(" ")
    inverse(true)
    print(" Sommaire ")
    underline(false)
    inverse(false)
    print(" or")
    underline(true)
    print(" ")
    inverse(true)
    print(" Retour ")
    underline(false)
    inverse(false)
    print(" to go")
    clearEndOfLine()

    moveCursor(0, SCREEN_HEIGHT_NORMAL - 1)
    colorForeground(5)
    colorBackground(1)
    print(" back to the main screen.")
    clearEndOfLine()
  }


  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    if (e !is Minitel.KeyboardEvent.FunctionKeyEvent) return
    when (e.functionKey) {
      FunctionKey.SOMMAIRE, FunctionKey.RETOUR -> onNavigateToMain()
      else -> {}
    }
  }
}
