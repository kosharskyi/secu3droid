/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/

package org.secu3.android.ui.parameters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.secu3.android.ui.parameters.pages.*

class ParametersPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return 17
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StarterFragment()
            1 -> AnglesFragment()
            2 -> IdlingFragment()
            3 -> FunctionsFragment()
            4 -> TemperatureFragment()
            5 -> FuelCutoffkFragment()
            6 -> AdcErrorsCorrectionsFragment()
            7 -> CkpsFragment()
            8 -> KnockFragment()
            9 -> MiscellaneousFragment()
            10 -> ChokeControlFragment()
            11 -> SecurityFragment()
            12 -> UniversalOutputsFragment()
            13 -> FuelInjectionFragment()
            14 -> LambdaControlFragment()
            15 -> AccelerationFragment()
            16 -> GasDoseFragment()
            else -> TODO()
        }
    }


}