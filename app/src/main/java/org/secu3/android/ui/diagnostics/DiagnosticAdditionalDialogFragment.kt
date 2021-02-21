package org.secu3.android.ui.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.navigation.navGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDiagnosticAdditionalDialogBinding

@AndroidEntryPoint
class DiagnosticAdditionalDialogFragment : DialogFragment() {

    private val mViewModel: DiagnosticsViewModel by navGraphViewModels(R.id.diagnosticsFragment, factoryProducer = { defaultViewModelProviderFactory })


    private lateinit var mBinding: FragmentDiagnosticAdditionalDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDiagnosticAdditionalDialogBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            mViewModel.outputPacket.let {
                enableTachO.isChecked = it.enableTachOtesting
                enableBlDe.isChecked = it.enableBlDeTesting

                okBtn.setOnClickListener { _ ->
                    it.enableBlDeTesting = enableBlDe.isChecked
                    it.enableTachOtesting = enableTachO.isChecked
                    mViewModel.toggleBlDe()
                    dismiss()
                }
                cancelBtn.setOnClickListener { dismiss() }
            }
        }

        mViewModel.firmwareLiveData.observe(viewLifecycleOwner) {
            mBinding.enableTachO.isVisible = it.isSecu3T.not()
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

}
