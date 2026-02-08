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
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.QuizConfiguration
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.QuizConfigured.Finished
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.QuizConfigured.InProgress
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.QuizConfigured.InProgress.Answering
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.QuizConfigured.InProgress.QuestionOutcome
import org.jraf.miniteljraf.app.francequiz.FranceQuizState.QuizIntro
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.printFormattedText
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
    when (state) {
      is QuizIntro -> drawQuizIntro()
      is QuizConfiguration -> drawQuizConfiguration()
      else -> drawQuestionAndAnswers()
    }
  }

  private suspend fun Minitel.Screen.drawQuizIntro() {
    moveCursor(0, HEADER_HEIGHT)
    val introText = """
      _Bienvenue sur France Quiz !_
      
      Ce quiz va tester vos connaissances sur la France à travers une série de questions à choix multiples.
      
      Il est conçu pour vous entraîner à _l'Examen Civique_, que doivent passer les personnes souhaitant obtenir la nationalité française.
      
      Les questions couvrent:
      - Les grands repères de l'histoire 
        de France
      - Les principes, symboles et
        institutions de la République
      - L'exercice de la citoyenneté française
      - La place de la France dans l'Europe et 
        dans le monde
    """.trimIndent()
    printFormattedText(text = introText)

    val footerY = SCREEN_HEIGHT_NORMAL - 1
    val suite = " SUITE "
    moveCursor(SCREEN_WIDTH_NORMAL - suite.length, footerY)
    color(background0To7 = 7, foreground0To7 = 0)
    print(suite)
  }

  private suspend fun Minitel.Screen.drawQuizConfiguration() {
    clearBelowHeader()
    moveCursor(0, HEADER_HEIGHT)
    val introText = """
      L'Examen Civique comporte _40 questions_, et pour le réussir il faut répondre correctement à au moins _32_ d'entre elles (40% de bonnes réponses).
      
      Pour ce quiz, vous pouvez choisir le nombre de questions que vous voulez affronter :
      
    """.trimIndent()
    printFormattedText(text = introText)
    print("\n")
    val answers = listOf("10 questions", "20 questions", "40 questions")
    printAnswers(answers)

    printYourChoice()
  }

  private suspend fun Minitel.Screen.printAnswers(answers: List<String>) {
    for ((answerIndex, answerText) in answers.withIndex()) {
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
      if (answerIndex < answers.lastIndex) {
        print("\n")
      }
    }
  }

  private suspend fun Minitel.Screen.printYourChoice() {
    val footerY = SCREEN_HEIGHT_NORMAL - 1
    val yourChoice = "Votre choix : "
    moveCursor(0, footerY)
    clearEndOfLine()
    colorForeground(6)
    print(yourChoice)
    val envoi = " ENVOI "
    moveCursor(SCREEN_WIDTH_NORMAL - envoi.length, footerY)
    color(background0To7 = 7, foreground0To7 = 0)
    print(envoi)
    showCursor(true)
    moveCursor(yourChoice.length, footerY)
    color(background0To7 = 0, foreground0To7 = 7)
  }

  private suspend fun Minitel.Screen.drawQuestionAndAnswers() {
    when (val state = state) {
      is QuizIntro, is QuizConfiguration -> throw IllegalStateException()
      is Answering -> {
        showCursor(false)
        drawProgress()
        clearBelowHeader()
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

        printAnswers(state.question.answers)

        printYourChoice()
      }

      is QuestionOutcome -> {
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
        val suite = " SUITE "
        moveCursor(SCREEN_WIDTH_NORMAL - suite.length, footerY)
        color(background0To7 = 7, foreground0To7 = 0)
        print(suite)
      }

      is Finished -> {
        showCursor(false)
        clearBelowHeader()
        moveCursor(0, HEADER_HEIGHT)
        val correctPercentage = state.score * 100 / state.questions.size
        val finishedText = """
          _Quiz terminé !_
          
          Votre score final est _${state.score}/${state.questions.size}_, soit _$correctPercentage%_ de bonnes réponses.
        """.trimIndent() + "\n\n" + if (correctPercentage >= 80) {
          """
            Félicitations, vous auriez réussi l'Examen Civique !
            
            N'hésitez pas à refaire le quiz pour vous entraîner encore !
          """.trimIndent()
        } else {
          """
            Malheureusement, avec moins de 80%, vous n'auriez pas réussi l'Examen Civique.
            
            N'hésitez pas à refaire le quiz pour vous entraîner !
          """.trimIndent()
        }
        printFormattedText(text = finishedText)

        print("\n\n\n\n")
        colorForeground(2)
        print("Réalisé par Benoît Lubek (BoD@JRAF.org)\net Carmen Alvarez (c@rmen.ca)")

        val footerY = SCREEN_HEIGHT_NORMAL - 1
        val retour = " RETOUR "
        moveCursor(SCREEN_WIDTH_NORMAL - retour.length, footerY)
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

  private suspend fun Minitel.Screen.clearBelowHeader() {
    moveCursor(0, 1)
    repeat(SCREEN_HEIGHT_NORMAL - 2) {
      print("\n")
      colorBackground(0)
      clearEndOfLine()
    }
  }


  override suspend fun onKeyboard(e: Minitel.KeyboardEvent) {
    when (val state = state) {
      is QuizIntro -> {
        if (e is Minitel.KeyboardEvent.FunctionKeyEvent && e.functionKey == FunctionKey.SUITE) {
          this.state = QuizConfiguration(questionCount = null)
          connection.screen.drawQuizConfiguration()
        }
      }

      is QuizConfiguration -> {
        when (e) {
          is Minitel.KeyboardEvent.CharacterEvent -> {
            if (state.questionCount != null) {
              // Answer already given
              connection.screen.beep()
              return
            }
            val inputChar = e.char.uppercaseChar()
            if (inputChar in 'A'..'C') {
              this.state = state.copy(
                questionCount = when (inputChar) {
                  'A' -> 10
                  'B' -> 20
                  'C' -> 40
                  else -> throw IllegalStateException()
                },
              )
              connection.screen.print(inputChar)
            } else {
              connection.screen.beep()
            }
          }

          is Minitel.KeyboardEvent.FunctionKeyEvent -> {
            when (e.functionKey) {
              FunctionKey.CORRECTION -> {
                if (state.questionCount == null) {
                  // No answer to correct
                  connection.screen.beep()
                  return
                }
                this.state = state.copy(questionCount = null)
                connection.screen.moveCursorLeft()
                connection.screen.print(' ')
                connection.screen.moveCursorLeft()
              }

              FunctionKey.ENVOI -> {
                if (state.questionCount != null) {
                  this.state = questionsState(state.questionCount)
                  connection.screen.drawQuestionAndAnswers()
                }
              }

              else -> {}
            }
          }
        }
      }


      is Answering -> {
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
                  this.state = QuestionOutcome(
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

      is QuestionOutcome -> {
        when (e) {
          is Minitel.KeyboardEvent.FunctionKeyEvent -> {
            when (e.functionKey) {
              FunctionKey.SUITE, FunctionKey.ENVOI -> {
                if (state.questionIndex < state.questions.lastIndex) {
                  this.state = Answering(
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

      is Finished -> {
        if (e is Minitel.KeyboardEvent.FunctionKeyEvent && e.functionKey == FunctionKey.RETOUR) {
          this.state = QuizConfiguration(null)
          connection.screen.drawScreen()
        }
      }
    }
  }
}
