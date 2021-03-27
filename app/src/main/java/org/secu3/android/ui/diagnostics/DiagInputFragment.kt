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
package org.secu3.android.ui.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDiagInputBinding


class DiagInputFragment : Fragment() {

    private lateinit var mBinding: FragmentDiagInputBinding

    private val mViewModel: DiagnosticsViewModel by navGraphViewModels(R.id.diagnosticsFragment, factoryProducer = { defaultViewModelProviderFactory })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDiagInputBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.diagInputLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {
                voltage.text = "%.3f".format(it.voltage)
                map.text = "%.3f".format(it.map)
                temp.text = "%.3f".format(it.temperature)
                add1.text = "%.3f".format(it.addI1)
                add2.text = "%.3f".format(it.addI2)
                add3.text = "%.3f".format(it.addI3)
                add4.text = "%.3f".format(it.addI4)
                carb.text = "%.3f".format(it.carb)
                ks1.text = "%.3f".format(it.ks1)
                ks2.text = "%.3f".format(it.ks2)

                gasV.isChecked = it.gasV
                ckps.isChecked = it.ckps
                refS.isChecked = it.refS
                ps.isChecked = it.ps
                bl.isChecked = it.bl
                de.isChecked = it.de

                add5.text = "%.3f".format(it.addI5)
                add6.text = "%.3f".format(it.addI6)
                add7.text = "%.3f".format(it.addI7)
                add8.text = "%.3f".format(it.addI8)
            }
        }
    }
}