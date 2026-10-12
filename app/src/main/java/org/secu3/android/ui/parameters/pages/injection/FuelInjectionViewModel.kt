/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android.ui.parameters.pages.injection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.secu3.android.models.packets.out.params.InjctrParPacket
import org.secu3.android.models.packets.out.params.InjctrParPacket.InjConfig.*

fun validInjectorConfigurations(cylinders: Int): List<Int> = buildList {
    add(THROTTLEBODY.id)
    add(SIMULTANEOUS.id)
    if (cylinders !in listOf(1, 2, 3, 5)) add(TWO_BANK_ALTERN.id)
    if (cylinders !in listOf(1, 3, 5)) { add(SEMISEQUENTIAL.id); add(SEMISEQSEPAR.id) }
    add(FULLSEQUENTIAL.id)
}

fun validInjectorPulses(config: Int, cylinders: Int): List<Int> = when (config) {
    THROTTLEBODY.id, SIMULTANEOUS.id -> when (cylinders) {
        1 -> listOf(1); 2 -> listOf(1, 2); 3 -> listOf(1, 3); 4 -> listOf(1, 2, 4)
        5 -> listOf(1, 5); 6 -> listOf(1, 2, 3, 6); 8 -> listOf(1, 2, 4, 8); else -> emptyList()
    }
    TWO_BANK_ALTERN.id -> when (cylinders) { 4 -> listOf(2, 4); 6 -> listOf(2, 6); 8 -> listOf(2, 4, 8); else -> emptyList() }
    SEMISEQUENTIAL.id, SEMISEQSEPAR.id -> when (cylinders) {
        2 -> listOf(1, 2); 4 -> listOf(2, 4); 6 -> listOf(3, 6); 8 -> listOf(4, 8); else -> emptyList()
    }
    FULLSEQUENTIAL.id -> listOf(cylinders).filter { it > 0 }
    else -> emptyList()
}

class FuelInjectionViewModel: ViewModel() {

    private var mSqrnum = listOf(
        MutableLiveData<List<Pair<Int, String>>>(),
        MutableLiveData<List<Pair<Int, String>>>()
    )
    val sqrnum: List<LiveData<List<Pair<Int, String>>>>
        get() = mSqrnum

    fun generateSquirtsDropDown(packet: InjctrParPacket, fi: Int) {
        require(fi in 0..1)
        mSqrnum[fi].value = emptyList()

        val config = if (fi == 0) packet.config0 else packet.config1

        //Fill squirts number list depending of selected configuration and number of engine cylinders
        if (config == THROTTLEBODY.id || config == SIMULTANEOUS.id) {

            when (packet.ckpsEngineCyl) {
                1 -> {
                    mSqrnum[fi].value = listOf(Pair(1, "1"))
                }

                2 -> {
                    mSqrnum[fi].value = listOf(
                        Pair(1, "1"),
                        Pair(2, "2"),
                    )
                }
                3 -> {
                    mSqrnum[fi].value = listOf(Pair(1, "1"), Pair(3, "3"))
                }
                4 -> mSqrnum[fi].value = listOf(
                    Pair(1, "1"),
                    Pair(2, "2"),
                    Pair(4, "4")
                )
                5 -> mSqrnum[fi].value = listOf(
                    Pair(1, "1"),
                    Pair(5, "5")
                )
                6 -> {
                    mSqrnum[fi].value = listOf(Pair(1, "1"), Pair(2, "2"), Pair(3, "3"), Pair(6, "6"))
                }
                8 -> mSqrnum[fi].value = listOf(
                    Pair(1, "1"),
                    Pair(2, "2"),
                    Pair(4, "4"),
                    Pair(8, "8"),
                )
            }
        }

        if (config == TWO_BANK_ALTERN.id) {

            when (packet.ckpsEngineCyl) {
                1, 2, 3, 5 -> mSqrnum[fi].value = listOf()
                4 -> mSqrnum[fi].value =
                    listOf(           //for 4 cyl engine this mode is the same as semi-sequential
                        Pair(2, "2"), Pair(4, "4"))
                6 -> mSqrnum[fi].value = listOf(Pair(2, "2"), Pair(6, "6"))
                8 -> mSqrnum[fi].value = listOf(Pair(2, "2"), Pair(4, "4"), Pair(8, "8"))
            }
        }

        if (config == SEMISEQUENTIAL.id || config == SEMISEQSEPAR.id) {
            when (packet.ckpsEngineCyl) {
                1, 3, 5 -> mSqrnum[fi].value = listOf()
                2 -> mSqrnum[fi].value = listOf(
                    Pair(1, "1"),
                    Pair(2, "2"),
                )
                4 -> mSqrnum[fi].value = listOf(
                    Pair(2, "2"),
                    Pair(4, "4"),
                )
                6 -> mSqrnum[fi].value = listOf(
                    Pair(3, "3"),
                    Pair(6, "6"),
                )
                8 -> mSqrnum[fi].value = listOf(
                    Pair(4, "4"),
                    Pair(8, "8"),
                )
            }
        }

        if (config == FULLSEQUENTIAL.id) {
            mSqrnum[fi].value = listOf(Pair(packet.ckpsEngineCyl, packet.ckpsEngineCyl.toString()))
        }
    }
}
