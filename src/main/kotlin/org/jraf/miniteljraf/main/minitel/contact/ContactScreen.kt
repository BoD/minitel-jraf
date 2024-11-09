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
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.main.minitel.ParameterlessJrafScreen
import org.jraf.miniteljraf.main.minitel.Resources
import org.jraf.miniteljraf.main.minitel.app.MinitelApp
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.printCentered
import org.jraf.miniteljraf.util.wrapped

private const val INPUT_HEIGHT = 3
private const val FOOTER_HEIGHT = 2
private val LOGO_AND_INTRO_HEIGHT = Resources.logoHeight + 4

class ContactScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onBack: suspend () -> Unit,
) : ParameterlessJrafScreen(context, connection) {
  private var input = ""
  private val messages = mutableListOf<Message>()

  override suspend fun start(startParameters: Unit) {
    connection.screen.drawScreen()
  }

  private suspend fun Minitel.Screen.drawScreen() {
    drawIntro()
    drawFooter()
    drawInputWindow()
    drawInput()
  }

  private suspend fun Minitel.Screen.drawIntro() {
    moveCursor(0, Resources.logoHeight)
    clearBottomOfScreen()
    moveCursor(3, Resources.logoHeight + 2)
    colorForeground(4)
    print("You can contact me at")
    colorForeground(7)
    underline(true)
    characterSize(CharacterSize.TALL)
    print(" BoD@JRAF.org")
    characterSize(CharacterSize.NORMAL)
    colorForeground(4)
    print('\n')
    printCentered("or leave me a message below!")
  }

  private suspend fun Minitel.Screen.drawFooter() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 2)
    color(1, 5)
    underline(true)
    print(" ")
    inverse(true)
    print(" Retour ")
    underline(false)
    inverse(false)
    print(" go back to the home screen")
    clearEndOfLine()

    moveCursor(0, SCREEN_HEIGHT_NORMAL - 1)
    color(1, 5)
    underline(true)
    print(" ")
    inverse(true)
    print(" Envoi ")
    underline(false)
    inverse(false)
    print(" send your message")
    clearEndOfLine()
  }

  private suspend fun Minitel.Screen.drawInputWindow() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - INPUT_HEIGHT - FOOTER_HEIGHT)
    inverse(true)
    color(background0To7 = 0, foreground0To7 = 3)
    clearEndOfLine()

    moveCursorDown()
    clearEndOfLine()

    moveCursorDown()
    clearEndOfLine()
  }

  private suspend fun Minitel.Screen.drawInput() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - INPUT_HEIGHT - FOOTER_HEIGHT)
    inverse(true)
    color(background0To7 = 0, foreground0To7 = 3)
    print(input)
    updateCursor()
  }

  private suspend fun Minitel.Screen.updateCursor() {
    showCursor(input.length < SCREEN_WIDTH_NORMAL * INPUT_HEIGHT)
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (e) {
      is Minitel.KeyboardEvent.CharacterEvent -> {
        if (e.char.isISOControl()) return
        if (input.length >= SCREEN_WIDTH_NORMAL * INPUT_HEIGHT) {
          connection.screen.beep()
          return
        }
        val c = e.char.invertCase()
        input += c
        connection.screen.print(c)

        connection.screen.updateCursor()
      }

      is Minitel.KeyboardEvent.FunctionKeyEvent -> {
        when (e.functionKey) {
          FunctionKey.SOMMAIRE, FunctionKey.RETOUR -> onBack()
          FunctionKey.CORRECTION -> {
            if (input.isNotEmpty()) {
              input = input.dropLast(1)
              connection.screen.moveCursorLeft()
              connection.screen.clearEndOfLine()

              connection.screen.updateCursor()
            }
          }

          FunctionKey.ANNULATION -> {
            input = ""
            connection.screen.drawInputWindow()
            connection.screen.drawInput()
            connection.screen.updateCursor()
          }

          FunctionKey.ENVOI -> {
            connection.screen.handleInput()
          }

          else -> {}
        }
      }
    }
  }

  private var bufferCursor = 0

  private suspend fun Minitel.Screen.drawMessages() {
    showCursor(false)
    val buffer = messages.toLines()
    val bufferHeight = SCREEN_HEIGHT_NORMAL - LOGO_AND_INTRO_HEIGHT - INPUT_HEIGHT - FOOTER_HEIGHT
    val bufferWindow = buffer.takeLast(bufferHeight)
    if (bufferWindow.size < bufferHeight) {
      val currentBufferCursor = bufferCursor
      for (i in bufferWindow.indices) {
        if (i >= currentBufferCursor) {
          moveCursor(0, i + LOGO_AND_INTRO_HEIGHT)
          clearEndOfLine()
          val line = bufferWindow[i]
          colorForeground(line.foregroundColor)
          print(line.text)
          bufferCursor++
        }
      }
    } else {
      for (i in bufferWindow.indices.reversed()) {
        moveCursor(0, i + LOGO_AND_INTRO_HEIGHT)
        clearEndOfLine()
        val line = bufferWindow[i]
        colorForeground(line.foregroundColor)
        print(line.text)
      }
    }
  }

  private suspend fun Minitel.Screen.handleInput() {
    val input = input
    this@ContactScreen.input = ""
    showCursor(false)
    drawInputWindow()
    messages += Message.Local(input)
    messages += Message.Remote("Thank you for your message!", "BoD")
    drawMessages()
    drawInput()
  }

  private fun Char.invertCase(): Char {
    return if (isLowerCase()) uppercaseChar() else lowercaseChar()
  }

  private sealed interface Message {
    class Local(val text: String) : Message
    class Remote(val text: String, author: String) : Message
  }

  private fun List<Message>.toLines(): List<Line> {
    return map {
      when (it) {
        is Message.Local -> Line(text = it.text, characterSize = CharacterSize.NORMAL, foregroundColor = 5)
        is Message.Remote -> Line(text = it.text, characterSize = CharacterSize.NORMAL, foregroundColor = 7)
      }
    }.flatMap { it.wrapped(SCREEN_WIDTH_NORMAL) }
  }
}
