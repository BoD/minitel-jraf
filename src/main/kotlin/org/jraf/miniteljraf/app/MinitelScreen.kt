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

package org.jraf.miniteljraf.app

import org.jraf.klibminitel.core.Minitel

abstract class MinitelScreen<C, P>(
  protected val context: C,
  protected val connection: Minitel.Connection,
) {
  abstract suspend fun start(startParameters: P)

  abstract suspend fun onKeyboard(e: Minitel.KeyboardEvent)

  open suspend fun stop() {}
}

abstract class ParameterlessMinitelScreen(
  connection: Minitel.Connection,
) : MinitelScreen<Unit, Unit>(Unit, connection) {

  final override suspend fun start(startParameters: Unit) {
    start()
  }

  abstract suspend fun start()
}
