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

package org.secu3.android.ui.firmware

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentFirmwareDialogBinding
import org.secu3.android.models.packets.FirmwareInfoPacket

@AndroidEntryPoint
class FirmwareDialogFragment : DialogFragment() {

    internal val mViewModel: FirmwareDialogViewModel by viewModels()

    private var mBinding: FragmentFirmwareDialogBinding? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentFirmwareDialogBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.firmwareLiveData.observe(viewLifecycleOwner) {
            mBinding?.fwInfo?.text = it.tag

            mBinding?.fwOptions?.text = createOptions(it).joinToString("\n")
        }
    }

    fun createOptions(fw: FirmwareInfoPacket): MutableList<String> {
        return mutableListOf(
        "isOBDsupport: ${fw.isObdSupported}",
        "isATMEGA1284: ${fw.isATMEGA1284}",
        "isSplitAngle: ${fw.isSplitAngle}",
        "isCamSyncSupported: ${fw.isCamSyncSupported}",
        "isDwellControlSupported: ${fw.isDwellControlSupported}",
        "isCoolingFanPwmEnabled: ${fw.isCoolingFanPwmEnabled}",
        "isRealTimeTablesEnabled: ${fw.isRealTimeTablesEnabled}",
        "isIccAvrCompiler: ${fw.isIccAvrCompiler}",
        "isAvrGccCompiler: ${fw.isAvrGccCompiler}",
        "isDebugVariablesEnabled: ${fw.isDebugVariablesEnabled}",
        "isPhaseSensorEnabled: ${fw.isPhaseSensorEnabled}",
        "isPhasedIgnitionEnabled: ${fw.isPhasedIgnitionEnabled}",
        "isFuelPumpEnabled: ${fw.isFuelPumpEnabled}",
        "isThermistorCsEnabled: ${fw.isThermistorCsEnabled}",
        "isSecu3T: ${fw.isSecu3T}",
        "isDiagnosticsEnabled: ${fw.isDiagnosticsEnabled}",
        "isHallOutputEnabled: ${fw.isHallOutputEnabled}",
        "isRev9Board: ${fw.isRev9Board}",
        "isStroboscopeEnabled: ${fw.isStroboscopeEnabled}",
        "isSmControlEnabled: ${fw.isSmControlEnabled}",
        "isVRef5VEnabled: ${fw.isVRef5VEnabled}",
        "isHallSyncEnabled: ${fw.isHallSyncEnabled}",
        "isUartBinaryEnabled: ${fw.isUartBinaryEnabled}",
        "isCkps2ChignEnabled: ${fw.isCkps2ChignEnabled}",
        "isATMEGA644: ${fw.isATMEGA644}",
        "isFuelInjectEnabled: ${fw.isFuelInjectEnabled}",
        "isGdControlEnabled: ${fw.isGdControlEnabled}",
        "isCarbAfrEnabled: ${fw.isCarbAfrEnabled}",
        "isCkpsNplus1Enabled: ${fw.isCkpsNplus1Enabled}",
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding = null
    }
}