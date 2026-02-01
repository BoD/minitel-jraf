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

package org.jraf.miniteljraf.main.minitel.francequiz

import kotlin.random.Random

class Question(
  val text: String,
  val answers: List<String>,
  val correctAnswerIndex: Int,
)

sealed interface FranceQuizState {
  val questions: List<Question>
  val score: Int

  sealed interface InProgress : FranceQuizState {
    override val questions: List<Question>
    override val score: Int
    val questionIndex: Int

    val question get() = questions[questionIndex]

    data class Answering(
      override val questions: List<Question>,
      override val questionIndex: Int,
      override val score: Int,

      val answer: Int?,
    ) : InProgress

    data class QuestionOutcome(
      override val questions: List<Question>,
      override val questionIndex: Int,
      override val score: Int,

      val answer: Int,
    ) : InProgress
  }

  data class Finished(
    override val questions: List<Question>,
    override val score: Int,
  ) : FranceQuizState
}


fun testState(): FranceQuizState = FranceQuizState.InProgress.Answering(
  questions = testQuestions(),
  questionIndex = 0,
  score = 0,
  answer = null,
)

private fun testQuestions(): List<Question> = listOf(
  Question(
    text = "What is the capital of France?",
    answers = listOf("Berlin", "Madrid", "Paris", "Rome"),
    correctAnswerIndex = 2,
  ),
  Question(
    text = "Which river flows through Paris?",
    answers = listOf("Thames", "Seine", "Danube", "Rhine"),
    correctAnswerIndex = 1,
  ),
  Question(
    text = "What is the national symbol of France?",
    answers = listOf("Eagle", "Lion", "Rooster", "Bear"),
    correctAnswerIndex = 2,
  ),
)

fun initialState(): FranceQuizState = FranceQuizState.InProgress.Answering(
  questions = loadQuestions(),
  questionIndex = 0,
  score = 0,
  answer = null,
)

private fun loadQuestions(): List<Question> {
  val jsonQuestions = loadJsonQuestions().questions.shuffled().take(40)
  return jsonQuestions.map { jsonQuestion ->
    val answers = jsonQuestion.wrongAnswers.shuffled().take(3).toMutableList()
    val correctAnswerIndex = Random.nextInt(0, answers.size + 1)
    answers.add(index = correctAnswerIndex, jsonQuestion.correctAnswers.shuffled().first())
    Question(
      text = jsonQuestion.question,
      answers = answers.map {
        @Suppress("DEPRECATION")
        it.capitalize()
      },
      correctAnswerIndex = correctAnswerIndex,
    )
  }
}
