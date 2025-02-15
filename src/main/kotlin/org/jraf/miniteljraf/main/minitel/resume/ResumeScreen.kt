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

package org.jraf.miniteljraf.main.minitel.resume

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.main.minitel.ParameterlessJrafScreen
import org.jraf.miniteljraf.main.minitel.app.MinitelApp
import org.jraf.miniteljraf.resume.type.Language
import org.jraf.miniteljraf.util.Line

class ResumeScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onBack: suspend () -> Unit,
) : ParameterlessJrafScreen(
  context = context,
  connection = connection,
) {
  private lateinit var resumeLines: List<Line>
  private var cursor = 0
  private val previousCursors = mutableListOf(0)
  private val resumeApi = ResumeApi()

  private var language: Language = Language.EN

  override suspend fun start() {
    resumeLines = resumeApi.getResume(Language.EN)
    connection.screen.drawScreen(withHeader = true)
  }

  private suspend fun Minitel.Screen.drawScreen(withHeader: Boolean) {
    if (withHeader) {
      clearScreenAndHome()
      drawHeader()
    }
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
    print(" Resume")
    clearEndOfLine()
    moveCursor(0, 2)
    colorForeground(5)
    repeatCharacter('~', SCREEN_WIDTH_NORMAL)
  }

  private suspend fun Minitel.Screen.clearText() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 3)
    repeat(SCREEN_HEIGHT_NORMAL - 5) {
      colorForeground(0)
      clearEndOfLine()
      moveCursorUp()
    }
  }

  private suspend fun Minitel.Screen.drawFooter() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 2)
    color(1, 5)
    when (language) {
      Language.EN -> {
        print(" Français")
        underline(true)
        print(" ")
        inverse(true)
        print(" F ")
        underline(false)
        inverse(false)
        print("  Up")
        underline(true)
        print(" ")
        inverse(true)
        print(" Retour ")
        underline(false)
        inverse(false)
        print(" Down")
        underline(true)
        print(" ")
        inverse(true)
        print(" Suite ")
        underline(false)
        inverse(false)
        print(" ")

        color(1, 5)
        print(" Back to main screen")
      }

      Language.FR -> {
        print(" English")
        underline(true)
        print(" ")
        inverse(true)
        print(" E ")
        underline(false)
        inverse(false)
        print("  Haut")
        underline(true)
        print(" ")
        inverse(true)
        print(" Retour ")
        underline(false)
        inverse(false)
        print(" Bas")
        underline(true)
        print(" ")
        inverse(true)
        print(" Suite ")
        underline(false)
        inverse(false)
        print(" ")

        color(1, 5)
        print(" Retour à l'accueil")
      }

      Language.UNKNOWN__ -> {}
    }
    color(1, 5)
    underline(true)
    print(" ")
    inverse(true)
    print(" Sommaire ")
    underline(false)
    inverse(false)
    print(" ")
    clearEndOfLine()
  }


  private suspend fun Minitel.Screen.drawText() {
    moveCursor(0, 3)
    var y = 3
    while (true) {
      val line = resumeLines.getOrNull(cursor) ?: break
      if (y + line.characterSize.characterHeight > SCREEN_HEIGHT_NORMAL - 2) {
        break
      }
      cursor++
      if (line.characterSize.characterHeight == 2) {
        moveCursorDown()
        y++
      }
      characterSize(line.characterSize)
      color(line.backgroundColor, line.foregroundColor)
      if (line.centered) {
        repeatCharacter(' ', line.centeredOffset)
      }
      print(line.text)
      if (line.centered && line.backgroundColor != 0) {
        clearEndOfLine()
      }
      if (line.needsNewLine) {
        characterSize(CharacterSize.NORMAL)
        print("\n")
      } else {
        if (line.characterSize.characterHeight == 2) {
          characterSize(CharacterSize.NORMAL)
          moveCursorUp()
        }
      }
      y++
    }
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (e) {
      is Minitel.KeyboardEvent.FunctionKeyEvent -> when (e.functionKey) {
        FunctionKey.SOMMAIRE -> onBack()
        FunctionKey.SUITE -> {
          if (cursor >= resumeLines.size) {
            return
          }
          previousCursors += cursor
          connection.screen.clearText()
          connection.screen.drawText()
        }

        FunctionKey.RETOUR -> {
          if (cursor == 0) {
            return
          }
          previousCursors.removeLast()
          cursor = previousCursors.last()
          connection.screen.clearText()
          connection.screen.drawText()
        }

        else -> {}
      }

      is Minitel.KeyboardEvent.CharacterEvent -> when (e.char) {
        'f', 'F' -> {
          language = Language.FR
          reloadResume()
        }

        'e', 'E' -> {
          language = Language.EN
          reloadResume()
        }

        else -> {}
      }

      else -> {}
    }
  }

  private suspend fun reloadResume() {
    resumeLines = resumeApi.getResume(language)
    cursor = 0
    connection.screen.clearText()
    connection.screen.drawScreen(withHeader = false)
  }
}
