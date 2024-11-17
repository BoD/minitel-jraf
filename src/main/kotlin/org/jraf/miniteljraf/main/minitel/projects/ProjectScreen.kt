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

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.github.GetRepositoriesQuery
import org.jraf.miniteljraf.main.minitel.JrafScreen
import org.jraf.miniteljraf.main.minitel.app.MinitelApp
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.escapeEmoji
import org.jraf.miniteljraf.util.toPlainText
import org.jraf.miniteljraf.util.wrapped

class ProjectScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onBack: suspend () -> Unit,
) : JrafScreen<GetRepositoriesQuery.Node>(
  context = context,
  connection = connection,
) {
  private lateinit var readmeLines: List<Line>
  private var cursor = 0
  private var pageSize: Int = 0

  override suspend fun start(startParameters: GetRepositoriesQuery.Node) {
    loadReadme(startParameters)
    connection.screen.drawScreen()
  }

  private suspend fun loadReadme(project: GetRepositoriesQuery.Node) {
    val readmeText = GitHubApi.getReadme(project.name)
    val plainText = readmeText.toPlainText().escapeEmoji()
    val textLines = plainText.lines()
    readmeLines = textLines.map { line ->
      if (line.startsWith("# ")) {
        Line(
          text = line.drop(2).trim(),
          characterSize = CharacterSize.DOUBLE,
          foregroundColor = 7,
        )
      } else if (line.startsWith("## ")) {
        Line(
          text = line.drop(3).trim(),
          characterSize = CharacterSize.TALL,
          foregroundColor = 6,
        )
      } else if (line.startsWith("### ")) {
        Line(
          text = line.drop(4).trim(),
          characterSize = CharacterSize.TALL,
          foregroundColor = 5,
        )
      } else if (line.startsWith("#### ")) {
        Line(
          text = line.drop(5).trim(),
          characterSize = CharacterSize.NORMAL,
          foregroundColor = 4,
        )
      } else {
        Line(
          text = line,
          characterSize = CharacterSize.NORMAL,
          foregroundColor = 4,
        )
      }
    }.flatMap { it.wrapped(SCREEN_WIDTH_NORMAL) }

  }

  private suspend fun Minitel.Screen.drawScreen() {
    moveCursor(0, 3)
    clearBottomOfScreen()
    drawFooter()
    drawText()
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
    print(" Up")
    underline(true)
    print(" ")
    inverse(true)
    print(" Retour ")
    underline(false)
    inverse(false)
    repeatCharacter(' ', 15)
    print("Down")
    underline(true)
    print(" ")
    inverse(true)
    print(" Suite ")
    underline(false)
    inverse(false)
    print(" ")

    color(1, 5)
    print(" Back to list")
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
    colorForeground(7)
    var y = 3
    val prevCursor = cursor
    while (true) {
      val line = readmeLines.getOrNull(cursor) ?: break
      if (y + line.characterSize.characterHeight > SCREEN_HEIGHT_NORMAL - 2) {
        break
      }
      cursor++
      if (line.characterSize.characterHeight == 2) {
        moveCursorDown()
        y++
      }
      characterSize(line.characterSize)
      colorForeground(line.foregroundColor)
      print(line.text)
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
    pageSize = cursor - prevCursor
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (e) {
      is Minitel.KeyboardEvent.FunctionKeyEvent -> when (e.functionKey) {
        FunctionKey.SOMMAIRE -> onBack()
        FunctionKey.SUITE -> {
          if (cursor >= readmeLines.size) {
            return
          }
          connection.screen.clearText()
          connection.screen.drawText()
        }

        FunctionKey.RETOUR -> {
          if (cursor == 0) {
            return
          }
          cursor -= (pageSize * 1.75).toInt()
          if (cursor < 0) {
            cursor = 0
          }
          connection.screen.clearText()
          connection.screen.drawText()
        }

        else -> {}
      }

      else -> {}
    }
  }
}
