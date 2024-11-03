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

package org.jraf.miniteljraf.util

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL

suspend fun Minitel.Screen.printCentered(s: String) {
  val padding = " ".repeat((SCREEN_WIDTH_NORMAL - s.length) / 2)
  repeatCharacter(' ', padding.length)
  print(s)
}

fun String.wrappedLines(maxWidth: Int = SCREEN_WIDTH_NORMAL): List<String> {
  val words = split(" ")
  val lines = mutableListOf<String>()
  var currentLine = ""
  for (word in words) {
    if (currentLine.isEmpty()) {
      currentLine = word
    } else {
      val newLine = "$currentLine $word"
      if (newLine.length > maxWidth) {
        lines.add(currentLine)
        currentLine = word
      } else {
        currentLine = newLine
      }
    }
  }
  if (currentLine.isNotEmpty()) {
    lines.add(currentLine.abbreviate(maxWidth))
  }
  return lines
}

fun String.abbreviate(maxWidth: Int = SCREEN_WIDTH_NORMAL): String {
  return if (length > maxWidth) {
    substring(0, maxWidth - 3) + "..."
  } else {
    this
  }
}

fun String.split(maxWidth: Int, firstLineMaxWidth: Int): List<String> {
  val lines = mutableListOf<String>()
  var currentLine = ""
  var currentMaxWidth = firstLineMaxWidth
  for (c in this) {
    if (currentLine.length + 1 > currentMaxWidth) {
      lines += currentLine
      currentLine = c.toString()
      currentMaxWidth = maxWidth
    } else {
      currentLine += c
    }
  }
  lines += currentLine
  return lines
}

data class Line(
  val text: String,
  val characterSize: CharacterSize,
  val foregroundColor: Int,
) {
  val needsNewLine = text.length < characterSize.maxCharactersHorizontal
}

fun Line.wrapped(maxWidth: Int = SCREEN_WIDTH_NORMAL): List<Line> {
  val actualMaxWidth = maxWidth / characterSize.characterWidth
  if (text.length <= actualMaxWidth) {
    return listOf(this)
  }
  val lines = mutableListOf<Line>()
  val words = text.split(" ").toMutableList()
  var currentLine = ""
  while (words.isNotEmpty()) {
    val word = words.removeAt(0)
    if (currentLine.isEmpty()) {
      currentLine = word
    } else {
      val newLine = "$currentLine $word"
      if (newLine.length > actualMaxWidth) {
        if (word.length > actualMaxWidth) {
          words.addAll(0, word.split(actualMaxWidth, actualMaxWidth - currentLine.length - 1))
          continue
        } else {
          lines.add(copy(text = currentLine))
          currentLine = word
        }
      } else {
        currentLine = newLine
      }
    }
  }
  if (currentLine.isNotEmpty()) {
    lines.add(copy(text = currentLine))
  }
  return lines
}
