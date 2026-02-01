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

package org.jraf.miniteljraf.main.minitel.francequiz

import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.main.minitel.ParameterlessJrafScreen
import org.jraf.miniteljraf.main.minitel.app.MinitelApp
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.wrapped

class FranceQuizScreen(
  context: MinitelApp.Context,
  connection: Minitel.Connection,
  private val onBack: suspend () -> Unit,
) : ParameterlessJrafScreen(context, connection) {
  private var state: FranceQuizState = initialState()

  override suspend fun start() {
    connection.screen.drawScreen()
  }

  private suspend fun Minitel.Screen.drawScreen() {
    when (val state = state) {
      is FranceQuizState.InProgress.Answering -> {
        showCursor(false)
        clearScreenAndHome()
        colorForeground(7)

        val questionTextLines = Line(state.question.text).wrapped()
        for (line in questionTextLines) {
          print(line.text)
          clearEndOfLine()
          if (line.needsNewLine) {
            print("\n")
          }
        }
        print("\n")

        colorForeground(5)
        for ((answerIndex, answerText) in state.question.answers.withIndex()) {
          val answerTextLines = Line("${'A' + answerIndex}. $answerText").wrapped(SCREEN_WIDTH_NORMAL - 1)
          for (line in answerTextLines) {
            print(" " + line.text)
            clearEndOfLine()
            if (line.needsNewLine) {
              print("\n")
            }
          }
          if (answerIndex < state.question.answers.lastIndex) {
            print("\n")
          }
        }

        val footerY = SCREEN_HEIGHT_NORMAL - 1
        val yourChoice = "Votre choix : "
        moveCursor(0, footerY)
        clearEndOfLine()
        colorForeground(6)
        print(yourChoice)
        val envoi = " ENVOI"
        moveCursor(SCREEN_WIDTH_NORMAL - envoi.length - 1, footerY)
        color(background0To7 = 7, foreground0To7 = 0)
        print(envoi)
        showCursor(true)
        moveCursor(yourChoice.length, footerY)
        color(background0To7 = 0, foreground0To7 = 7)
      }

      is FranceQuizState.InProgress.QuestionOutcome -> {
        showCursor(false)
        val questionTextLines = Line(state.question.text).wrapped()
        moveCursor(0, questionTextLines.size + 1)

        for ((answerIndex, answerText) in state.question.answers.withIndex()) {
          val answerTextLines = Line(
            text = "${'A' + answerIndex}. $answerText",
            foregroundColor = when (answerIndex) {
              state.question.correctAnswerIndex -> 0
              state.answer -> 1
              else -> 5
            },
            backgroundColor = when (answerIndex) {
              state.question.correctAnswerIndex -> 7
              else -> 0
            },
          ).wrapped(SCREEN_WIDTH_NORMAL - 1)
          for (line in answerTextLines) {
            color(background0To7 = line.backgroundColor, foreground0To7 = line.foregroundColor)
            print(" " + line.text)
            clearEndOfLine()
            if (line.needsNewLine) {
              print("\n")
            }
          }
          if (answerIndex < state.question.answers.lastIndex) {
            print("\n")
          }
        }

        val footerY = SCREEN_HEIGHT_NORMAL - 1
        val outcome = if (state.answer == state.question.correctAnswerIndex) {
          "Bonne réponse ! Score : ${state.score}/${state.questions.size}"
        } else {
          "Mauvaise réponse. Score : ${state.score}/${state.questions.size}"
        }
        moveCursor(0, footerY)
        clearEndOfLine()
        colorForeground(6)
        print(outcome)
        val suite = " SUITE"
        moveCursor(SCREEN_WIDTH_NORMAL - suite.length - 1, footerY)
        color(background0To7 = 7, foreground0To7 = 0)
        print(suite)
      }

      is FranceQuizState.Finished -> {
        showCursor(false)
        clearScreenAndHome()
        colorForeground(6)

        val finishedText = "Quiz terminé ! Votre score final est ${state.score}/${state.questions.size}."
        val finishedTextLines = Line(finishedText, centered = true).wrapped()
        for (line in finishedTextLines) {
          print(line.text)
          clearEndOfLine()
          if (line.needsNewLine) {
            print("\n")
          }
        }
        print("\n")

        val footerY = SCREEN_HEIGHT_NORMAL - 1
        val retour = " RETOUR"
        moveCursor(SCREEN_WIDTH_NORMAL - retour.length - 1, footerY)
        color(background0To7 = 7, foreground0To7 = 0)
        print(retour)
      }
    }
  }

  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (val state = state) {
      is FranceQuizState.InProgress.Answering -> {
        when (e) {
          is Minitel.KeyboardEvent.CharacterEvent -> {
            if (state.answer != null) {
              // Answer already given
              connection.screen.beep()
              return
            }
            val inputChar = e.char.uppercaseChar()
            val answerIndex = inputChar - 'A'
            if (answerIndex in state.question.answers.indices) {
              this.state = state.copy(answer = answerIndex)
              connection.screen.print(inputChar)
            } else {
              connection.screen.beep()
            }
          }

          is Minitel.KeyboardEvent.FunctionKeyEvent -> {
            when (e.functionKey) {
              FunctionKey.CORRECTION -> {
                if (state.answer == null) {
                  // No answer to correct
                  connection.screen.beep()
                  return
                }
                this.state = state.copy(answer = null)
                connection.screen.moveCursorLeft()
                connection.screen.print(' ')
                connection.screen.moveCursorLeft()
              }

              FunctionKey.ENVOI -> {
                if (state.answer != null) {
                  this.state = FranceQuizState.InProgress.QuestionOutcome(
                    questions = state.questions,
                    questionIndex = state.questionIndex,
                    score = state.score + if (state.answer == state.question.correctAnswerIndex) 1 else 0,
                    answer = state.answer,
                  )
                  connection.screen.drawScreen()
                }
              }

              FunctionKey.SOMMAIRE -> onBack()
              else -> {}
            }
          }
        }
      }

      is FranceQuizState.InProgress.QuestionOutcome -> {
        when (e) {
          is Minitel.KeyboardEvent.FunctionKeyEvent -> {
            when (e.functionKey) {
              FunctionKey.SUITE -> {
                if (state.questionIndex < state.questions.lastIndex) {
                  this.state = FranceQuizState.InProgress.Answering(
                    questions = state.questions,
                    questionIndex = state.questionIndex + 1,
                    score = state.score,
                    answer = null,
                  )
                  connection.screen.drawScreen()
                } else {
                  this.state = FranceQuizState.Finished(
                    questions = state.questions,
                    score = state.score,
                  )
                  connection.screen.drawScreen()
                }
              }

              FunctionKey.SOMMAIRE -> onBack()
              else -> {}
            }
          }

          else -> {}
        }
      }

      else -> {}
    }
  }
}
