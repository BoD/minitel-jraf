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

package org.jraf.miniteljraf.main.minitel.main

import kotlinx.coroutines.delay
import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.main.minitel.JrafScreen
import org.jraf.miniteljraf.main.minitel.Resources
import org.jraf.miniteljraf.main.minitel.app.MinitelApp
import org.jraf.miniteljraf.main.minitel.main.MainScreen.StartMode.CLEAR_AND_ANIMATE_LOGO
import org.jraf.miniteljraf.main.minitel.main.MainScreen.StartMode.CLEAR_AND_DRAW_LOGO
import org.jraf.miniteljraf.main.minitel.main.MainScreen.StartMode.CLEAR_AND_KEEP_LOGO
import org.jraf.miniteljraf.util.printCentered
import kotlin.random.Random

class MainScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onNavigateToContact: suspend () -> Unit,
  private val onNavigateToProjects: suspend () -> Unit,
  private val onNavigateToPlayStore: suspend () -> Unit,
  private val onNavigateToResume: suspend () -> Unit,
  private val onNavigateToMastodon: suspend () -> Unit,
) : JrafScreen<MainScreen.StartMode>(context, connection) {
  val yourChoiceLabel = "Your choice: "
  val envoiLabel = " Envoi "

  enum class StartMode {
    CLEAR_AND_ANIMATE_LOGO,
    CLEAR_AND_DRAW_LOGO,
    CLEAR_AND_KEEP_LOGO,
  }

  override suspend fun start(startParameters: StartMode) {
    input = ""
    connection.screen.drawScreen(startParameters)
  }

  private suspend fun Minitel.Screen.drawScreen(startMode: StartMode) {
    when (startMode) {
      CLEAR_AND_ANIMATE_LOGO -> {
        clearScreenAndHome()
        drawLogo(animate = true)
      }

      CLEAR_AND_DRAW_LOGO -> {
        clearScreenAndHome()
        drawLogo(animate = false)
      }

      CLEAR_AND_KEEP_LOGO -> {
        moveCursor(0, Resources.logoHeight)
        clearBottomOfScreen()
      }
    }
    drawIntroText()
    drawMenu()
    drawTextEntrySection()
  }

  private suspend fun Minitel.Screen.drawLogo(animate: Boolean) {
    if (animate) {
      moveCursor(0, SCREEN_HEIGHT_NORMAL / 2 - Resources.logoHeight / 2)
    }
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
    if (animate) {
      // Wait for the logo to be displayed
      delay(3200)
      // Animate (scroll up)
      moveCursor(0, SCREEN_HEIGHT_NORMAL - 1)
      repeat(SCREEN_HEIGHT_NORMAL / 2 - Resources.logoHeight / 2) {
        delay(100)
        raw('\n')
      }
    }
  }

  private suspend fun Minitel.Screen.drawIntroText() {
    moveCursor(0, Resources.logoHeight + 1)
    colorForeground(5)
    printCentered("Welcome to Benoît \"BoD\" Lubek's\n")
    printCentered("personal minitel site.")
  }

  private suspend fun Minitel.Screen.drawMenu() {
    moveCursor(3, 10)
    menuItem("1", "Contact")

    moveCursor(24, 12)
    menuItem("2", "Projects")

    moveCursor(5, 14)
    menuItem("3", "Play Store")

    moveCursor(21, 16)
    menuItem("4", "Resume")

    moveCursor(1, 18)
    menuItem("5", "Mastodon")
  }

  private suspend fun Minitel.Screen.menuItem(shortcut: String, caption: String) {
    val bgColor = Random.nextInt(5) + 2
    val fgColor = if (bgColor < 4) 7 else 0
    color(bgColor, fgColor)
    characterSize(CharacterSize.TALL)
    print(" $shortcut ")
    color(0, Random.nextInt(5) + 2)
    print(" $caption")
  }

  var input = ""

  private suspend fun Minitel.Screen.drawTextEntrySection() {
    moveCursor(1, SCREEN_HEIGHT_NORMAL - 2)
    colorForeground(4)
    print(yourChoiceLabel)
    colorForeground(6)
    printDots()
    underline(true)
    print(" ")
    inverse(true)
    print(envoiLabel)
    moveCursor(1 + yourChoiceLabel.length, SCREEN_HEIGHT_NORMAL - 2)
    showCursor(true)
  }

  private suspend fun Minitel.Screen.printDots() {
    val dotsLength = SCREEN_WIDTH_NORMAL - yourChoiceLabel.length - 1 - envoiLabel.length - 2
    colorForeground(2)
    repeatCharacter('.', dotsLength)
  }

  private suspend fun Minitel.Screen.petitCoquin() {
    moveCursor(0, -1)
    colorForeground(7)
    print("Petit coquin !")
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (e) {
      is Minitel.KeyboardEvent.CharacterEvent -> {

        if (e.char.isISOControl()) return
        input += e.char
        connection.screen.print(e.char)
      }

      is Minitel.KeyboardEvent.FunctionKeyEvent -> {
        when (e.functionKey) {
          FunctionKey.CORRECTION -> {
            if (input.isNotEmpty()) {
              connection.screen.showCursor(false)
              input = input.dropLast(1)
              connection.screen.moveCursorLeft()
              connection.screen.colorForeground(2)
              connection.screen.print('.')
              connection.screen.moveCursorLeft()
              connection.screen.colorForeground(7)
              connection.screen.showCursor(true)
            }
          }

          FunctionKey.ENVOI -> {
            when (val i = input.lowercase()) {
              "1", "contact" -> onNavigateToContact()
              "2", "projects" -> onNavigateToProjects()
              "3", "play store", "play", "store" -> onNavigateToPlayStore()
              "4", "resume" -> onNavigateToResume()
              "5", "mastodon" -> onNavigateToMastodon()
              else -> {
                connection.screen.showCursor(false)
                if (i == "ulla") {
                  connection.screen.petitCoquin()
                }
                connection.screen.beep()
                input = ""
                connection.screen.moveCursor(1 + yourChoiceLabel.length, SCREEN_HEIGHT_NORMAL - 2)
                connection.screen.printDots()
                connection.screen.moveCursor(1 + yourChoiceLabel.length, SCREEN_HEIGHT_NORMAL - 2)
                connection.screen.colorForeground(7)
                connection.screen.showCursor(true)
              }
            }
          }

          else -> {}
        }
      }
    }
  }

  override suspend fun stop() {
    connection.screen.showCursor(false)
  }
}
