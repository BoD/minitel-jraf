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

package org.jraf.miniteljraf.main.minitel

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.miniteljraf.util.printCentered
import kotlin.random.Random

private val logoHeight = Resources.logo3615Lines.size

suspend fun Minitel.Connection.MainScreen() {
  screen.drawScreen()

  keyboard.collect { key ->
    screen.print("You pressed: $key")
  }
}

private suspend fun Minitel.Screen.drawScreen() {
  drawLogo()
  drawIntroText()
  drawMenu()
}

private suspend fun Minitel.Screen.drawLogo() {
  moveCursor(0, SCREEN_HEIGHT_NORMAL / 2 - logoHeight / 2)
  for ((index, logo3615Line) in Resources.logo3615Lines.withIndex()) {
    graphicsMode(true)
    print(" ")
    raw(logo3615Line)
    graphicsMode(false)
    color(0, 7)
    print(" ")
    print(Resources.logoJrafLines[index])
    print('\n')
  }

  // Animate (scroll up)
  moveCursor(0, SCREEN_HEIGHT_NORMAL - 1)
  raw("\n".repeat(SCREEN_HEIGHT_NORMAL / 2 - logoHeight / 2))
}

private suspend fun Minitel.Screen.drawIntroText() {
  moveCursor(0, logoHeight + 1)
  colorForeground(5)
  printCentered("Welcome to Benoît \"BoD\" Lubek's\n")
  printCentered("personal minitel site.")
}

private suspend fun Minitel.Screen.drawMenu() {
  moveCursor(3, 10)
  menuItem("1", "Contact")

  moveCursor(24, 12)
  menuItem("2", "GitHub")

  moveCursor(5, 16)
  menuItem("3", "Play Store")

  moveCursor(21, 19)
  menuItem("4", "Resume")

  moveCursor(1, 22)
  menuItem("5", "Mastodon")
}

private suspend fun Minitel.Screen.menuItem(shortcut: String, caption: String) {
  val bgColor = Random.nextInt(7) + 1
  val fgColor = if (bgColor < 4) 7 else 0
  color(bgColor, fgColor)
  characterSize(CharacterSize.TALL)
  print(" $shortcut ")
  color(0, Random.nextInt(7) + 1)
  print(" $caption")
}
