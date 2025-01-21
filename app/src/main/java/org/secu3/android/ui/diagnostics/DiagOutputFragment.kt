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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDiagOutputBinding

class DiagOutputFragment : Fragment() {

    private val mViewModel: DiagnosticsViewModel by navGraphViewModels(R.id.diagnosticsFragment, factoryProducer = { defaultViewModelProviderFactory })

    private lateinit var mBinding: FragmentDiagOutputBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDiagOutputBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.firmwareInfo?.let {
            mBinding.apply {
                secu3t.isVisible = it.isSecu3T
                secu3i.isVisible = it.isSecu3T.not()
            }
        }

        mViewModel.enableBlDe.observe(viewLifecycleOwner) {
            mBinding.apply {
                bl.isEnabled = it
                de.isEnabled = it
            }
        }

        mViewModel.enableTachO.observe(viewLifecycleOwner) {
            mBinding.tachO.isVisible = it
        }

        mBinding.apply {
            ignOut1.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ignOut1 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            ignOut2.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ignOut2 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            ignOut3.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ignOut3 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            ignOut4.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ignOut4 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            ignOut5.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ignOut5 = isChecked
                mViewModel.sendDiagOutPacket()
            }

            addIo1.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.addIo1 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            addIo2.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.addIo2 = isChecked
                mViewModel.sendDiagOutPacket()
            }

            ie.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ie = isChecked
                mViewModel.sendDiagOutPacket()
            }
            fe.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.fe = isChecked
                mViewModel.sendDiagOutPacket()
            }
            ecf.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ecf = isChecked
                mViewModel.sendDiagOutPacket()
            }
            ecf0.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ecf0 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            ce.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.ce = isChecked
                mViewModel.sendDiagOutPacket()
            }
            stBlock.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.stBlock = isChecked
                mViewModel.sendDiagOutPacket()
            }
            bl.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.bl = isChecked
                mViewModel.sendDiagOutPacket()
            }
            de.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.de = isChecked
                mViewModel.sendDiagOutPacket()
            }

            injO1.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.injO1 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            injO2.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.injO2 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            injO3.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.injO3 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            injO4.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.injO4 = isChecked
                mViewModel.sendDiagOutPacket()
            }
            injO5.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.injO5 = isChecked
                mViewModel.sendDiagOutPacket()
            }

            stblO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.stblO = isChecked
                mViewModel.sendDiagOutPacket()
            }
            celO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.celO = isChecked
                mViewModel.sendDiagOutPacket()
            }
            fpmpO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.fpmpO = isChecked
                mViewModel.sendDiagOutPacket()
            }
            pwrrO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.pwrrO = isChecked
                mViewModel.sendDiagOutPacket()
            }
            evapO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.evapO = isChecked
                mViewModel.sendDiagOutPacket()
            }
            o2shO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.o2shO = isChecked
                mViewModel.sendDiagOutPacket()
            }
            condO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.condO = isChecked
                mViewModel.sendDiagOutPacket()
            }
            addO2.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.addO2 = isChecked
                mViewModel.sendDiagOutPacket()
            }

            tachO.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.tachO = isChecked
                mViewModel.sendDiagOutPacket()
            }

            gpa6O.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.outputPacket.tachO = isChecked
                mViewModel.sendDiagOutPacket()
            }
        }
    }
}