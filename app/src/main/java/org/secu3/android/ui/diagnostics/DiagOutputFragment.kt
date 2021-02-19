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

        mViewModel.firmwareLiveData.observe(viewLifecycleOwner) {
            mBinding.secu3t.isVisible = it.isSecu3T
            mBinding.secu3i.isVisible = it.isSecu3T.not()
        }

        mViewModel.enableBlDe.observe(viewLifecycleOwner) {
            checkBlDeTachOutputs()
        }

        mBinding.ignOut1.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ignOut1 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.ignOut2.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ignOut2 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.ignOut3.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ignOut3 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.ignOut4.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ignOut4 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.ignOut5.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ignOut5 = isChecked
            mViewModel.sendDiagOutPacket()
        }

        mBinding.addIo1.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.addIo1 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.addIo2.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.addIo2 = isChecked
            mViewModel.sendDiagOutPacket()
        }

        mBinding.ie.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ie = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.fe.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.fe = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.ecf.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ecf = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.ce.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.ce = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.stBlock.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.stBlock = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.bl.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.bl = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.de.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.de = isChecked
            mViewModel.sendDiagOutPacket()
        }

        mBinding.injO1.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.injO1 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.injO2.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.injO2 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.injO3.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.injO3 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.injO4.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.injO4 = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.injO5.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.injO5 = isChecked
            mViewModel.sendDiagOutPacket()
        }

        mBinding.stblO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.stblO = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.celO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.celO = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.fpmpO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.fpmpO = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.pwrrO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.pwrrO = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.evapO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.evapO = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.o2shO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.o2shO = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.condO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.condO = isChecked
            mViewModel.sendDiagOutPacket()
        }
        mBinding.addO2.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.addO2 = isChecked
            mViewModel.sendDiagOutPacket()
        }

        mBinding.tachO.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.outputPacket.tachO = isChecked
            mViewModel.sendDiagOutPacket()
        }

        checkBlDeTachOutputs()
    }

    private fun checkBlDeTachOutputs() {
        mBinding.bl.isVisible = mViewModel.outputPacket.enableBlDeTesting
        mBinding.de.isVisible = mViewModel.outputPacket.enableBlDeTesting
        mBinding.tachO.isVisible = mViewModel.outputPacket.enableTachOtesting
    }
}