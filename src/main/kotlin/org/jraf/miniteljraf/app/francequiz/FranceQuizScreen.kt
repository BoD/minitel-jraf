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

package org.jraf.miniteljraf.app.francequiz

import org.jraf.klibminitel.core.CharacterSize
import org.jraf.klibminitel.core.FunctionKey
import org.jraf.klibminitel.core.Minitel
import org.jraf.klibminitel.core.SCREEN_HEIGHT_NORMAL
import org.jraf.klibminitel.core.SCREEN_WIDTH_NORMAL
import org.jraf.miniteljraf.app.ParameterlessMinitelScreen
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.Finished
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.InProgress
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.wrapped

private const val HEADER_HEIGHT = 2

class FranceQuizScreen(
  connection: Minitel.Connection,
) : ParameterlessMinitelScreen(connection) {
  private var state: FranceQuizState = initialState()

  override suspend fun start() {
    connection.screen.drawScreen()
  }

  private suspend fun Minitel.Screen.drawScreen() {
    clearScreenAndHome()
    drawHeader()
    drawQuestionAndAnswers()
  }

  private suspend fun Minitel.Screen.drawQuestionAndAnswers() {
    when (val state = state) {
      is InProgress.Answering -> {
        showCursor(false)
        drawProgress()
        clearQuestionAndAnswers()
        moveCursor(0, HEADER_HEIGHT)
        colorForeground(7)
        val questionTextLines = Line(state.question.text).wrapped()
        for (line in questionTextLines) {
          print(line.text)
          if (line.needsNewLine) {
            print("\n")
          }
        }
        print("\n")

        for ((answerIndex, answerText) in state.question.answers.withIndex()) {
          val answerTextLines = Line("${'A' + answerIndex}. $answerText").wrapped(SCREEN_WIDTH_NORMAL - 1)
          for ((lineIndex, line) in answerTextLines.withIndex()) {
            if (lineIndex == 0) {
              print(" ")
              colorForeground(7)
              inverse(true)
              print(line.text.take(2))
              colorForeground(5)
              inverse(false)
              print(line.text.drop(2))
            } else {
              colorForeground(5)
              print(" " + line.text)
            }
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

      is InProgress.QuestionOutcome -> {
        showCursor(false)
        val questionTextLines = Line(state.question.text).wrapped()
        moveCursor(0, questionTextLines.size + 1 + HEADER_HEIGHT)

        val answerLines = state.question.answers.withIndex().map {
          val (answerIndex, answerText) = it
          Line(
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
        }

        val wrongAnswerIndex = if (state.answer != state.question.correctAnswerIndex) {
          state.answer
        } else {
          null
        }
        if (wrongAnswerIndex != null) {
          val y = questionTextLines.size + 1 + HEADER_HEIGHT + (0..<wrongAnswerIndex).sumOf { answerLines[it].size + 1 }
          moveCursor(0, y)
          val wrongAnswerLines = answerLines[wrongAnswerIndex]
          for (line in wrongAnswerLines) {
            color(line.backgroundColor, line.foregroundColor)
            print(" " + line.text)
            if (line.needsNewLine) {
              print("\n")
            }
          }
        }

        val correctAnswerIndex = state.question.correctAnswerIndex
        val y = questionTextLines.size + 1 + HEADER_HEIGHT + (0..<correctAnswerIndex).sumOf { answerLines[it].size + 1 }
        moveCursor(0, y)
        val correctAnswerLines = answerLines[correctAnswerIndex]
        for (line in correctAnswerLines) {
          color(line.backgroundColor, line.foregroundColor)
          print(" ")
          clearEndOfLine()
          print(line.text)
          clearEndOfLine()
          if (line.needsNewLine) {
            print("\n")
          }
        }

        val footerY = SCREEN_HEIGHT_NORMAL - 1
        val goodAnswer = state.answer == state.question.correctAnswerIndex
        val outcome = if (goodAnswer) {
          "Bonne réponse !"
        } else {
          "Mauvaise réponse."
        }
        val score = "Score : ${state.score}/${state.questions.size}"
        moveCursor(0, footerY)
        clearEndOfLine()
        colorForeground(if (goodAnswer) 7 else 2)
        print(outcome)
        print(" ")
        colorForeground(5)
        print(score)
        val suite = " SUITE"
        moveCursor(SCREEN_WIDTH_NORMAL - suite.length - 1, footerY)
        color(background0To7 = 7, foreground0To7 = 0)
        print(suite)
      }

      is Finished -> {
        showCursor(false)
        clearScreenAndHome()
        colorForeground(6)

        val finishedText = "Quiz terminé ! Votre score final est ${state.score}/${state.questions.size}."
        val finishedTextLines = Line(finishedText, centered = true).wrapped()
        for (line in finishedTextLines) {
          print(line.text)
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

  private suspend fun Minitel.Screen.drawHeader() {
    color(background0To7 = 5, foreground0To7 = 0)
    print(" ")
    characterSize(CharacterSize.WIDE)
    print("France Quiz")
    characterSize(CharacterSize.NORMAL)
    clearEndOfLine()
  }

  private suspend fun Minitel.Screen.drawProgress() {
    val progress = " ${(state as InProgress).questionIndex + 1}/${(state as InProgress).questions.size}"
    moveCursor(SCREEN_WIDTH_NORMAL - progress.length - 1, 0)
    colorForeground(1)
    print(progress)
  }

  private suspend fun Minitel.Screen.clearQuestionAndAnswers() {
    moveCursor(0, 2)
    repeat(SCREEN_HEIGHT_NORMAL - 2) {
      clearEndOfLine()
      print("\n")
    }
  }


  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (val state = state) {
      is InProgress.Answering -> {
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
                  this.state = InProgress.QuestionOutcome(
                    questions = state.questions,
                    questionIndex = state.questionIndex,
                    score = state.score + if (state.answer == state.question.correctAnswerIndex) 1 else 0,
                    answer = state.answer,
                  )
                  connection.screen.drawQuestionAndAnswers()
                }
              }

              else -> {}
            }
          }
        }
      }

      is InProgress.QuestionOutcome -> {
        when (e) {
          is Minitel.KeyboardEvent.FunctionKeyEvent -> {
            when (e.functionKey) {
              FunctionKey.SUITE, FunctionKey.ENVOI -> {
                if (state.questionIndex < state.questions.lastIndex) {
                  this.state = InProgress.Answering(
                    questions = state.questions,
                    questionIndex = state.questionIndex + 1,
                    score = state.score,
                    answer = null,
                  )
                  connection.screen.drawQuestionAndAnswers()
                } else {
                  this.state = Finished(
                    questions = state.questions,
                    score = state.score,
                  )
                  connection.screen.drawQuestionAndAnswers()
                }
              }

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
