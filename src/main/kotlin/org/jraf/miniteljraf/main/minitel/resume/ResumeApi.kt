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

import com.apollographql.apollo.ApolloClient
import org.jraf.klibminitel.core.CharacterSize
import org.jraf.miniteljraf.resume.ResumeQuery
import org.jraf.miniteljraf.resume.type.Language
import org.jraf.miniteljraf.util.Line
import org.jraf.miniteljraf.util.wrapped

class ResumeApi {
  private val apolloClient = ApolloClient.Builder()
    .serverUrl("http://server.jraf.org:4000")
    .build()

  private val introEn = """
    Here is my resume. You can also find it in PDF and other forms at https://JRAF.org/blubek
  """.trimIndent()

  private val introFr = """
    Voici mon CV. Vous pouvez aussi le trouver en PDF et autres formats sur https://JRAF.org/blubek
  """.trimIndent()

  suspend fun getResume(language: Language): List<Line> {
    val data = apolloClient.query(ResumeQuery(language = language)).execute().dataOrThrow()
    return (
      intro(language) +
        identity(data) +
        emptyLine() +
        Line(
          text = data.resume.title,
          characterSize = CharacterSize.TALL,
          foregroundColor = 7,
          centered = true,
        ) +
        emptyLine() +
        employment(data, language) +
        emptyLine() +
        skills(data, language) +
        emptyLine() +
        education(data, language) +
        emptyLine() +
        misc(data, language)
      ).flatMap { it.wrapped() }
  }

  private fun intro(language: Language) = listOf(
    Line(
      text = when (language) {
        Language.EN -> introEn
        Language.FR -> introFr
        else -> throw IllegalStateException()
      },
      characterSize = CharacterSize.NORMAL,
      foregroundColor = 4,
    ),
    emptyLine(),
  )

  private fun identity(data: ResumeQuery.Data) = listOf(
    Line(
      text = data.resume.identity.firstName + " " + data.resume.identity.lastName,
      characterSize = CharacterSize.TALL,
      foregroundColor = 6,
    ),
    Line(
      text = data.resume.identity.email,
      characterSize = CharacterSize.NORMAL,
      foregroundColor = 5,
    ),
    Line(
      text = data.resume.identity.phoneNumber.countryCode?.let { "+$it " }.orEmpty() + data.resume.identity.phoneNumber.number,
      characterSize = CharacterSize.NORMAL,
      foregroundColor = 5,
    ),
  )

  private fun employment(data: ResumeQuery.Data, language: Language): List<Line> {
    return listOf(
      Line(
        text = when (language) {
          Language.EN -> "EMPLOYMENT HISTORY"
          Language.FR -> "EXPÉRIENCE"
          else -> throw IllegalStateException()
        },
        characterSize = CharacterSize.TALL,
        foregroundColor = 6,
        backgroundColor = 1,
        centered = true,
      ),
      emptyLine(),
    ) +
      data.resume.experience.take(3).flatMap { experience ->
        listOf(
          Line(
            text = experience.period.start.date + " - " + (experience.period.end.date ?: "Present"),
            characterSize = CharacterSize.WIDE,
            foregroundColor = 5,
          ),
          emptyLine(),
          Line(
            text = experience.jobTitle + " at " + experience.organization.name + " (" + experience.organization.moreInfo?.let { "$it, " }
              .orEmpty() + experience.organization.description + ", " + experience.organization.location + ")",
            characterSize = CharacterSize.NORMAL,
            foregroundColor = 7,
          ),
          emptyLine(),
        ) +
          experience.items.map { item ->
            Line(
              text = "- " + item.description,
              characterSize = CharacterSize.NORMAL,
              foregroundColor = 4,
            )
          } +
          emptyLine() +
          Line(
            text = when (language) {
              Language.EN -> "Environment: "
              Language.FR -> "Environnement : "
              else -> throw IllegalStateException()
            } + experience.environment.joinToString(", "),
            characterSize = CharacterSize.NORMAL,
            foregroundColor = 5,
          ) +
          emptyLine()
      }
  }

  private fun skills(data: ResumeQuery.Data, language: Language): List<Line> {
    return listOf(
      Line(
        text = when (language) {
          Language.EN -> "SKILLS"
          Language.FR -> "COMPÉTENCES"
          else -> throw IllegalStateException()
        },
        characterSize = CharacterSize.TALL,
        foregroundColor = 6,
        backgroundColor = 1,
        centered = true,
      ),
      emptyLine(),
    ) +
      data.resume.skills.flatMap { skill ->
        listOf(
          Line(
            text = skill.name,
            characterSize = CharacterSize.NORMAL,
            foregroundColor = 7,
          ),
        ) +
          skill.items.map { item ->
            Line(
              text = (if (skill.items.size > 1) "- " else "") + item.description,
              characterSize = CharacterSize.NORMAL,
              foregroundColor = 4,
            )
          } +
          emptyLine()
      }
  }

  private fun education(data: ResumeQuery.Data, language: Language): List<Line> {
    return listOf(
      Line(
        text = when (language) {
          Language.EN -> "EDUCATION"
          Language.FR -> "FORMATION"
          else -> throw IllegalStateException()
        },
        characterSize = CharacterSize.TALL,
        foregroundColor = 6,
        backgroundColor = 1,
        centered = true,
      ),
      emptyLine(),
    ) +
      data.resume.education.flatMap { education ->
        val periodOrYear = if (education.periodOrYear.onPeriod != null) {
          education.periodOrYear.onPeriod.start.date + " - " + (education.periodOrYear.onPeriod.end.date ?: "Present")
        } else {
          education.periodOrYear.onYear!!.year!!
        }
        listOf(
          Line(
            text = periodOrYear,
            characterSize = CharacterSize.WIDE,
            foregroundColor = 5,
          ),
          Line(
            text = education.degree + education.institution?.let { " at " + it.name }.orEmpty(),
            characterSize = CharacterSize.NORMAL,
            foregroundColor = 7,
          ),
        ) +
          buildList {
            if (education.moreInfo != null) {
              add(
                Line(
                  text = education.moreInfo,
                  characterSize = CharacterSize.NORMAL,
                  foregroundColor = 5,
                ),
              )
            }
          } +
          emptyLine()
      }
  }

  private fun misc(data: ResumeQuery.Data, language: Language): List<Line> {
    return listOf(
      Line(
        text = when (language) {
          Language.EN -> "INTERESTS & HOBBIES"
          Language.FR -> "LOISIRS"
          else -> throw IllegalStateException()
        },
        characterSize = CharacterSize.TALL,
        foregroundColor = 6,
        backgroundColor = 1,
        centered = true,
      ),
      emptyLine(),
    ) +
      data.resume.misc.flatMap { misc ->
        listOf(
          Line(
            text = misc.description,
            characterSize = CharacterSize.NORMAL,
            foregroundColor = 5,
          ),
        ) +
          emptyLine()
      }
  }

  private fun emptyLine() = Line(
    text = "",
    characterSize = CharacterSize.NORMAL,
    foregroundColor = 0,
  )
}
