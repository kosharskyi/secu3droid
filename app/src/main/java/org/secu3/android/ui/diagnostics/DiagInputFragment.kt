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
package org.secu3.android.ui.diagnostics

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDiagInputBinding
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.views.FloatParamView


class DiagInputFragment : Fragment() {

    private lateinit var mBinding: FragmentDiagInputBinding

    private val mViewModel: DiagnosticsViewModel by navGraphViewModels(R.id.diagnosticsFragment, factoryProducer = { defaultViewModelProviderFactory })

    private val secu3TchannelValues = listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IE", "FE", "ECF", "CE", "ST_BLOCK", "ADD_IO1", "ADD_IO2", "BL", "DE")
    private val secu3IChannelValues = listOf("NONE", "IGN_OUT1", "IGN_OUT2", "IGN_OUT3", "IGN_OUT4", "IGN_OUT5", "ECF", "INJ_O1", "INJ_O2", "INJ_O3", "INJ_O4", "INJ_O5", "BL", "DE", "STBL_O", "CEL_O", "FPMP_O", "PWRR_O", "EVAP_O", "O2SH_0", "COND_O", "ADD_O2", "TACH_O", "GPA6_O")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDiagInputBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.firmwareInfo?.let {
            mBinding.apply {
                secu3iGroup.isVisible = it.isSecu3T.not()

                val chList = if (it.isSecu3T) secu3TchannelValues else secu3IChannelValues

                selectOutput.inputType = InputType.TYPE_NULL
                ArrayAdapter(requireContext(), R.layout.list_item, chList).also {
                    selectOutput.setAdapter(it)
                    selectOutput.setText(chList[0], false)
                }
                selectOutput.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    mViewModel.outputPacket.chan = position
                    mViewModel.sendDiagOutPacket()
                }

                freq.addOnValueChangeListener {
                    mViewModel.outputPacket.frq = it
                    if (mViewModel.outputPacket.chan > 0) {
                        mViewModel.sendDiagOutPacket()
                    }
                }
                freq.setOnClickListener { floatParamClick(it as FloatParamView) }


                duty.addOnValueChangeListener {
                    mViewModel.outputPacket.duty = it
                    if (mViewModel.outputPacket.chan > 0) {
                        mViewModel.sendDiagOutPacket()
                    }
                }
                duty.setOnClickListener { floatParamClick(it as FloatParamView) }

            }
        }

        mViewModel.diagInputLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {
                voltage.value = it.voltage
                map.value = it.map
                temp.value = it.temperature
                add1.value = it.addI1
                add2.value = it.addI2
                add3.value = it.addI3
                add4.value = it.addI4
                carb.value = it.carb
                ks1.value = it.ks1
                ks2.value = it.ks2

                gasV.isChecked = it.gasV
                ckps.isChecked = it.ckps
                refS.isChecked = it.refS
                ps.isChecked = it.ps
                bl.isChecked = it.bl
                de.isChecked = it.de
                condI.isChecked = it.cond_i
                epasI.isChecked = it.epas_i
                ignI.isChecked = it.ign_i
                gpa4I.isChecked = it.gpa4_i

                add5.value = it.addI5
                add6.value = it.addI6
                add7.value = it.addI7
                add8.value = it.addI8
            }
        }
    }

    // NOTE: make sure you've changed this method in [BaseParamFragment.kt]
    private fun floatParamClick(view: FloatParamView) {
        view.apply {
            ParamFloatEditDialogFragment.newInstance(value, title, step, maxValue, minValue, precision).also {
                it.newValueLiveData.observe(viewLifecycleOwner) { result ->
                    value = result
                }

                it.show(childFragmentManager, it::class.java.simpleName)
            }
        }
    }
}