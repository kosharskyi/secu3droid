/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */
package org.secu3.android.ui.parameters

internal sealed interface ParameterEditRequest {
    val title: String

    data class IntValue(
        override val title: String,
        val value: Int,
        val min: Int,
        val max: Int,
        val step: Int,
        val onValue: (Int) -> Unit,
    ) : ParameterEditRequest

    data class FloatValue(
        override val title: String,
        val value: Float,
        val min: Float,
        val max: Float,
        val step: Float,
        val precision: Int,
        val onValue: (Float) -> Unit,
    ) : ParameterEditRequest
}

internal data class ParameterEditSession(val id: Long, val request: ParameterEditRequest)
