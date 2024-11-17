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
import org.jraf.miniteljraf.main.minitel.JrafScreen
import org.jraf.miniteljraf.main.minitel.app.MinitelApp
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.wrapped

class MastodonScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onBack: suspend () -> Unit,
) : JrafScreen<Boolean>(context, connection) {
  private var shouldClearScreen = true
  private var page = 0
  private var lastShowedPosts: List<MastodonApi.Post> = emptyList()

  override suspend fun start(startParameters: Boolean) {
    connection.screen.drawScreen(startParameters)
  }

  private suspend fun Minitel.Screen.drawScreen(shouldRedrawFooter: Boolean) {
    if (shouldClearScreen) {
      clearScreenAndHome()
      drawHeader()
      drawFooter()
    }
    if (shouldRedrawFooter) {
      drawFooter()
    }
    if (page == 0) {
      if (!shouldClearScreen) {
        clearPosts()
      }
      drawText()
    } else {
      clearPosts()
    }
    drawPosts()
    shouldClearScreen = false
  }

  private suspend fun Minitel.Screen.drawHeader() {
    moveCursor(0, 1)
    characterSize(CharacterSize.TALL)
    colorForeground(5)
    colorBackground(1)
    print(" 3615 JRAF |")
    colorForeground(7)
    print(" Mastodon")
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
        I don't post a lot on Mastodon, but hereare the latest things I've shared.

        See more at
      """.trimIndent(),
    )
    colorForeground(7)
    underline(true)
    print(" https://mastodon.social/@BoD")
  }

  private suspend fun Minitel.Screen.drawFooter() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 2)
    color(1, 5)
    print(" Prev. page")
    underline(true)
    print(" ")
    inverse(true)
    print(" Retour ")
    underline(false)
    inverse(false)
    repeatCharacter(' ', 3)
    print("Next page")
    underline(true)
    print(" ")
    inverse(true)
    print(" Suite ")
    underline(false)
    inverse(false)

    print(" Back to main screen")
    underline(true)
    print(" ")
    inverse(true)
    print(" Sommaire ")
    underline(false)
    inverse(false)
    print(" ")
    clearEndOfLine()
  }

  private suspend fun Minitel.Screen.drawPosts() {
    var y = if (page == 0) 8 else 3
    moveCursor(0, y)
    val posts = MastodonApi.getPosts(after = lastShowedPosts.lastOrNull())
    var index = 0
    var post: MastodonApi.Post
    while (true) {
      post = posts[index]
      val textLines = Line(post.text, CharacterSize.NORMAL, 0).wrapped(SCREEN_WIDTH_NORMAL - 1)
      val postHeight = 2 + textLines.size
      if (y + postHeight > SCREEN_HEIGHT_NORMAL - 2) {
        lastShowedPosts += posts[index - 1]
        break
      }
      y += postHeight

      val bg = if (index % 2 == 0) 2 else 3
      color(bg, 7)
      print(" ${post.createdAt}")
      colorForeground(0)
      clearEndOfLine()
      print('\n')
      for (line in textLines) {
        color(bg, 0)
        print(" " + line.text)
        clearEndOfLine()
        if (line.needsNewLine) {
          print("\n")
        }
      }
      print('\n')
      index++
      if (index > posts.lastIndex) {
        lastShowedPosts += posts.last()
        break
      }
    }
  }

  private suspend fun Minitel.Screen.clearPosts() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 3)
    repeat(SCREEN_HEIGHT_NORMAL - 5) {
      colorForeground(0)
      clearEndOfLine()
      moveCursorUp()
    }
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (e) {
      is Minitel.KeyboardEvent.FunctionKeyEvent -> when (e.functionKey) {
        FunctionKey.SOMMAIRE -> onBack()
        FunctionKey.RETOUR -> if (page == 0) {
          onBack()
        } else {
          page--
          lastShowedPosts = lastShowedPosts.dropLast(2)
          connection.screen.drawScreen(shouldRedrawFooter = false)
        }

        FunctionKey.SUITE -> {
          if (MastodonApi.getPosts(after = lastShowedPosts.lastOrNull()).isEmpty()) {
            return
          }
          page++
          connection.screen.drawScreen(shouldRedrawFooter = false)
        }

        else -> {}
      }

      else -> {}
    }
  }
}
