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

package org.secu3.android.ui.parameters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.ui.parameters.pages.*

class ParametersPagerAdapter(fragment: Fragment, fwInfoPacket: FirmwareInfoPacket?) : FragmentStateAdapter(fragment) {

    private val pages: List<BaseParamFragment>

    init {
        val list =  mutableListOf(
            StarterFragment(),
            AnglesFragment(),
            IdlingFragment(),
            FunctionsFragment(),
            TemperatureFragment(),
            FuelCutoffkFragment(),
            AdcErrorsCorrectionsFragment(),
            CkpsFragment(),
            KnockFragment(),
            MiscellaneousFragment(),
            ChokeControlFragment(),
            SecurityFragment(),
            UniversalOutputsFragment(),
        )

        fwInfoPacket?.let {
            if (it.isFuelInjectEnabled) {
                list.add(FuelInjectionFragment())
            }

            if (it.isFuelInjectEnabled || it.isCarbAfrEnabled || it.isGdControlEnabled) {
                list.add(LambdaControlFragment())
            }

            if (it.isFuelInjectEnabled || it.isGdControlEnabled) {
                list.add(AccelerationFragment())
            }

            if (it.isGdControlEnabled) {
                list.add(GasDoseFragment())
            }

            if (it.isFuelInjectEnabled) {
                list.add(LtftFragment())
            }

            if (it.isSecu3T) {
                list.add(DbwFragment())
            }
        }
        pages = list
    }

    val titles: List<Int>
        get() = pages.map { it.tabTitle() }

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun createFragment(position: Int): Fragment {
        return pages[position]
    }


}