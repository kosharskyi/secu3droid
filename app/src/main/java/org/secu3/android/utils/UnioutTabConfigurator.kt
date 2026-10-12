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
package org.secu3.android.utils

internal data class UniOutConditionFormat(
    val min: Float, val max: Float, val step: Float, val precision: Int,
    val onDefault: Float, val offDefault: Float
)

internal fun uniOutConditionFormat(id: Int): UniOutConditionFormat = when (id) {
    0, 14, 28, 30 -> UniOutConditionFormat(-40f, 300f, .25f, 2, 55f, 50f)
    1 -> UniOutConditionFormat(50f, 30000f, 10f, 0, 1500f, 1200f)
    2, 29, 35, 41 -> UniOutConditionFormat(.25f, 500f, .25f, 2, 95f, 90f)
    3 -> UniOutConditionFormat(5f, 16f, .1f, 1, 10f, 10.5f)
    4, 17, 19, 31, 32 -> UniOutConditionFormat(0f, 1f, 1f, 0, 0f, 1f)
    5 -> UniOutConditionFormat(5f, 250f, .1f, 1, 70f, 65f)
    6 -> UniOutConditionFormat(0f, 16f, 1f, 0, 13f, 12f)
    7, 8, 9, 20 -> UniOutConditionFormat(0f, 600f, .1f, 1, 0f, 5f)
    10 -> UniOutConditionFormat(0f, 100f, .5f, 1, 60f, 55f)
    11 -> UniOutConditionFormat(-15f, 65f, .1f, 1, 55f, 53f)
    12 -> UniOutConditionFormat(0f, 5f, .01f, 2, 2.5f, 2.45f)
    15, 21, 22, 24, 25, 26, 27 -> UniOutConditionFormat(0f, 5f, .01f, 2, 2.5f, 2.48f)
    16 -> UniOutConditionFormat(0f, 600f, .1f, 2, 2.5f, 2.48f)
    13 -> UniOutConditionFormat(0f, 100f, .5f, 1, 30f, 29f)
    18 -> UniOutConditionFormat(.01f, 200f, .01f, 2, 20f, 19.9f)
    23 -> UniOutConditionFormat(0f, 600f, .01f, 2, 1f, 5f)
    33 -> UniOutConditionFormat(.02f, 650f, .1f, 2, 95f, 90f)
    34 -> UniOutConditionFormat(-1000f, 1000f, 1f, 0, 55f, 50f)
    36 -> UniOutConditionFormat(0f, 700f, .25f, 2, 320f, 300f)
    37 -> UniOutConditionFormat(0f, 25f, .05f, 3, 4.1f, 4f)
    38 -> UniOutConditionFormat(0f, 2000f, .25f, 1, 1000f, 990f)
    39, 40 -> UniOutConditionFormat(-40f, 250f, .25f, 1, 100f, 98f)
    else -> UniOutConditionFormat(0f, 1f, 1f, 0, 0f, 1f)
}
