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
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSecurityBinding
import org.secu3.android.ui.parameters.ParamsViewModel


class SecurityFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentSecurityBinding

    private val btTypes = listOf("BC417", "BK3231")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSecurityBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.bluetoothType.inputType = InputType.TYPE_NULL
        ArrayAdapter(requireContext(), R.layout.list_item, btTypes).also {
            mBinding.bluetoothType.setAdapter(it)
        }

        mViewModel.securityLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {
                bluetoothType.setText(btTypes[it.btType], false)

                useBluetooth.isChecked = it.useBt
                useImmobilizer.isChecked = it.useImmobilizer

                loadParamsFromFlash.isChecked = it.useReserveParams
                checkFirmwareIntegrity.isChecked = it.checkFwCrc
            }
        }
    }
}