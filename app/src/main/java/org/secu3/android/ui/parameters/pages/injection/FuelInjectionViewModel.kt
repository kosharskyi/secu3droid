/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2021 Vitaliy O. Kosharskiy. Ukraine, Kharkiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2021 Alexey A. Shabelnikov. Ukraine, Kyiv
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
import org.secu3.android.models.packets.params.InjctrParPacket
import org.secu3.android.models.packets.params.InjctrParPacket.Companion.INJCFG_2BANK_ALTERN
import org.secu3.android.models.packets.params.InjctrParPacket.Companion.INJCFG_FULLSEQUENTIAL
import org.secu3.android.models.packets.params.InjctrParPacket.Companion.INJCFG_SEMISEQSEPAR
import org.secu3.android.models.packets.params.InjctrParPacket.Companion.INJCFG_SEMISEQUENTIAL
import org.secu3.android.models.packets.params.InjctrParPacket.Companion.INJCFG_SIMULTANEOUS
import org.secu3.android.models.packets.params.InjctrParPacket.Companion.INJCFG_THROTTLEBODY

class FuelInjectionViewModel: ViewModel() {

    private var mSqrnum = listOf(
        MutableLiveData<List<Pair<Int, String>>>(),
        MutableLiveData<List<Pair<Int, String>>>()
    )
    val sqrnum: List<LiveData<List<Pair<Int, String>>>>
        get() = mSqrnum

    fun generateSquirtsDropDown(packet: InjctrParPacket, fi: Int) {

        val config = if (fi == 0) packet.config0 else packet.config1

        //Fill squirts number list depending of selected configuration and number of engine cylinders
        if (config == INJCFG_THROTTLEBODY || config == INJCFG_SIMULTANEOUS) {

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

        if (config == INJCFG_2BANK_ALTERN) {

            when (packet.ckpsEngineCyl) {
                1, 2, 3, 5 -> mSqrnum[fi].value = listOf()
                4 -> mSqrnum[fi].value =
                    listOf(           //for 4 cyl engine this mode is the same as semi-sequential
                        Pair(2, "2"), Pair(4, "4"))
                6 -> mSqrnum[fi].value = listOf(Pair(2, "2"), Pair(6, "6"))
                8 -> mSqrnum[fi].value = listOf(Pair(2, "2"), Pair(4, "4"), Pair(8, "8"))
            }
        }

        if (config == INJCFG_SEMISEQUENTIAL || config == INJCFG_SEMISEQSEPAR) {
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

        if (config == INJCFG_FULLSEQUENTIAL) {
            mSqrnum[fi].value = listOf(Pair(packet.ckpsEngineCyl, packet.ckpsEngineCyl.toString()))
        }
    }
}
