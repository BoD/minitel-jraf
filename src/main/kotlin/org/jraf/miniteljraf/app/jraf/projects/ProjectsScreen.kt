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

package org.jraf.miniteljraf.app.jraf.projects

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.app.MinitelScreen
import org.jraf.miniteljraf.app.jraf.JrafMinitelApp
import org.jraf.miniteljraf.github.GetRepositoriesQuery
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.abbreviate
import org.jraf.miniteljraf.util.wrapped

class ProjectsScreen(
  context: JrafMinitelApp.Context,
  connection: Minitel.Connection,
  private val onBack: suspend () -> Unit,
  private val onNavigateToProject: suspend (GetRepositoriesQuery.Node) -> Unit,
) : MinitelScreen<JrafMinitelApp.Context, Boolean>(context, connection) {
  private var shouldClearScreen = true
  private var page = 0
  private var lastShowedRepos: List<GetRepositoriesQuery.Node> = emptyList()
  private val letterToRepository = mutableMapOf<Char, GetRepositoriesQuery.Node>()
  private val gitHubApi = GitHubApi()

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
        clearProjects()
      }
      drawText()
    } else {
      clearProjects()
    }
    drawProjects()
    shouldClearScreen = false
  }

  private suspend fun Minitel.Screen.drawHeader() {
    moveCursor(0, 1)
    characterSize(CharacterSize.TALL)
    colorForeground(5)
    colorBackground(1)
    print(" 3615 JRAF |")
    colorForeground(7)
    print(" Projects")
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
        Here are few little projects I've been
        working on. Some of them are even main-
        tained, I swear!

        See more at:
      """.trimIndent(),
    )
    colorForeground(7)
    underline(true)
    print(" https://github.com/BoD")
  }

  private suspend fun Minitel.Screen.drawFooter() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 2)
    color(1, 5)
    print(" Home")
    underline(true)
    print(" ")
    inverse(true)
    print(" Somm. ")
    underline(false)
    inverse(false)
    print(" Prev")
    underline(true)
    print(" ")
    inverse(true)
    print(" Retour ")
    underline(false)
    inverse(false)
    print(" Next")
    underline(true)
    print(" ")
    inverse(true)
    print(" Suite ")
    underline(false)
    inverse(false)

    color(1, 5)
    print(" or press a letter for more details.")
    clearEndOfLine()
  }

  private suspend fun Minitel.Screen.drawProjects() {
    var y = if (page == 0) 9 else 3
    moveCursor(0, y)
    val repositories = gitHubApi.getRepositories(after = lastShowedRepos.lastOrNull())
    var index = 0
    var repository: GetRepositoriesQuery.Node
    letterToRepository.clear()
    while (true) {
      repository = repositories[index]
      val descriptionLines = Line(repository.description ?: "(no description)", CharacterSize.NORMAL, 0).wrapped(SCREEN_WIDTH_NORMAL - 1)
      val repositoryHeight = 3 + descriptionLines.size
      if (y + repositoryHeight > SCREEN_HEIGHT_NORMAL - 2) {
        lastShowedRepos += repositories[index - 1]
        break
      }
      y += repositoryHeight

      print('\n')
      val bg = if (index % 2 == 0) 1 else 2
      characterSize(CharacterSize.TALL)
      color(bg, 7)
      val repositoryName = repository.name.abbreviate(SCREEN_WIDTH_NORMAL - 4)
      print(" $repositoryName")
      colorForeground(0)
      repeatCharacter(' ', SCREEN_WIDTH_NORMAL - repositoryName.length - 4)
      colorBackground(3)
      val letter = index.toChar() + 'A'.code
      print(" $letter")
      clearEndOfLine()
      characterSize(CharacterSize.NORMAL)
      print('\n')
      for (line in descriptionLines) {
        color(bg, 0)
        print(" " + line.text)
        clearEndOfLine()
        if (line.needsNewLine) {
          print("\n")
        }
      }
      val text =
        "${repository.stargazerCount} star${if (repository.stargazerCount == 0 || repository.stargazerCount > 1) "s" else ""} - ${repository.forkCount} fork${if (repository.forkCount == 0 || repository.forkCount > 1) "s" else ""}"
      colorBackground(bg)
      colorForeground(5)
      repeatCharacter(' ', SCREEN_WIDTH_NORMAL - text.length)
      print("${repository.stargazerCount}")
      colorForeground(0)
      print(" star${if (repository.stargazerCount == 0 || repository.stargazerCount > 1) "s" else ""}")
      print(" - ")
      colorForeground(5)
      print("${repository.forkCount}")
      colorForeground(0)
      print(" fork${if (repository.forkCount == 0 || repository.forkCount > 1) "s" else ""}")

      letterToRepository[letter] = repository

      index++
      if (index > repositories.lastIndex) {
        lastShowedRepos += repositories.last()
        break
      }
    }
  }

  private suspend fun Minitel.Screen.clearProjects() {
    moveCursor(0, SCREEN_HEIGHT_NORMAL - 3)
    repeat(SCREEN_HEIGHT_NORMAL - 5) {
      colorForeground(0)
      clearEndOfLine()
      moveCursorUp()
    }
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (e) {
      is Minitel.KeyboardEvent.CharacterEvent -> {
        val repository = letterToRepository[e.char.uppercase().first()] ?: return
        lastShowedRepos = lastShowedRepos.dropLast(1)
        onNavigateToProject(repository)
      }

      is Minitel.KeyboardEvent.FunctionKeyEvent -> when (e.functionKey) {
        FunctionKey.SOMMAIRE -> onBack()
        FunctionKey.RETOUR -> if (page == 0) {
          onBack()
        } else {
          page--
          lastShowedRepos = lastShowedRepos.dropLast(2)
          connection.screen.drawScreen(shouldRedrawFooter = false)
        }

        FunctionKey.SUITE -> {
          if (gitHubApi.getRepositories(after = lastShowedRepos.lastOrNull()).isEmpty()) {
            return
          }
          page++
          connection.screen.drawScreen(shouldRedrawFooter = false)
        }

        else -> {}
      }
    }
  }
}
