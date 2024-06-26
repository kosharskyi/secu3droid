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
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSecurityBinding
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


class SecurityFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentSecurityBinding

    private val btTypes = listOf("BC417", "BK3231", "BK3231S(JDY-31)", "BC352(HC-05)", "BK3432", "BK3431S")

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

        lifecycleScope.launch {
            withResumed {
                mViewModel.securityLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    mBinding.apply {

                        progressBar.gone()
                        params.visible()

                        bluetoothType.setText(btTypes[it.btType], false)

                        useBluetooth.isChecked = it.useBt
                        useImmobilizer.isChecked = it.useImmobilizer

                        loadParamsFromFlash.isChecked = it.useReserveParams
                        checkFirmwareIntegrity.isChecked = it.checkFwCrc
                    }

                    mViewModel.isSendAllowed = true
                }
            }
        }
    }
}