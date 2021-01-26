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
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.secu3.android.databinding.FragmentAdcErrorsCorrectionsBinding
import org.secu3.android.ui.parameters.ParamsViewModel

class AdcErrorsCorrectionsFragment : Fragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentAdcErrorsCorrectionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentAdcErrorsCorrectionsBinding.inflate(inflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.adcCorrectionsLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {
                mapFactor.text = "%.3f".format(it.mapAdcFactor)
                mapCorrection.text = "%.4f".format(it.mapAdcCorrection)

                voltageFactor.text = "%.3f".format(it.ubatAdcFactor)
                voltageCorrection.text = "%.4f".format(it.ubatAdcCorrection)

                ctsFactor.text = "%.3f".format(it.tempAdcFactor)
                ctsCorrection.text = "%.4f".format(it.tempAdcCorrection)

                tpsFactor.text = "%.3f".format(it.tpsAdcFactor)
                tpsCorrection.text = "%.4f".format(it.tpsAdcCorrection)

                add1Factor.text = "%.3f".format(it.ai1AdcFactor)
                add1Correction.text = "%.4f".format(it.ai1AdcCorrection)

                add2Factor.text = "%.3f".format(it.ai2AdcFactor)
                add2Correction.text = "%.4f".format(it.ai2AdcCorrection)

                add3Factor.text = "%.3f".format(it.ai3AdcFactor)
                add3Correction.text = "%.4f".format(it.ai3AdcCorrection)

                add4Factor.text = "%.3f".format(it.ai4AdcFactor)
                add4Correction.text = "%.4f".format(it.ai4AdcCorrection)

            }
        }
    }
}