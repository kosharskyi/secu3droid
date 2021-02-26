/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2021 Vitaliy O. Kosharskiy. Ukraine, Kharkiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv
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

package org.secu3.android.ui.bluetoothStatus

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.databinding.FragmentBluetoothStatusBinding
import org.secu3.android.ui.settings.SettingsActivity
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible


@AndroidEntryPoint
class BluetoothStatusFragment : Fragment() {

    private lateinit var mBinding: FragmentBluetoothStatusBinding

    private val mViewModel: BluetoothStatusViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentBluetoothStatusBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {

            btDisabledButton.setOnClickListener {
                checkBtConfig()
            }

            btSettingsButton.setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }

            btUuidButton.setOnClickListener {
                startActivity(Intent(context, SettingsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkBtConfig()
    }

    private fun checkBtConfig() {
        mBinding.apply {
            btDisabledGroup.gone()
            btSettingsGroup.gone()
            btUuidGroup.gone()

            when {
                mViewModel.isBtEnabled().not() -> btDisabledGroup.visible()
                mViewModel.isBtDeviceAddressNotSelected() -> btSettingsGroup.visible()
                mViewModel.isBtDeviceNotExist() -> btUuidGroup.visible()
            }


        }

        if (mViewModel.isBtConfigured) {
            findNavController().navigate(BluetoothStatusFragmentDirections.actionOpenSensors())
        }
    }
}