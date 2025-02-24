package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import org.secu3.android.R
import org.secu3.android.databinding.FragmentLtftBinding
import org.secu3.android.models.packets.out.params.LtftParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class LtftFragment : BaseParamFragment() {

    private lateinit var mBinding: FragmentLtftBinding

    private var packet: LtftParamPacket? = null

    private val modeItems : List<String> by lazy {
        resources.getStringArray(R.array.ltft_mode_items).toList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentLtftBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            ltftMode.inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, modeItems).also {
                ltftMode.setAdapter(it)
            }
        }

        mViewModel.ltftLiveData.observe(viewLifecycleOwner) {
            mViewModel.isSendAllowed = false

            packet = it

            mBinding.apply {

                progressBar.gone()
                params.visible()

                ltftMode.setText(modeItems[it.mode], false)

                ltftLowerCltThreshold.value = it.learnClt
                ltftUpperCltThreshold.value = it.learnCltUp
                ltftUpperIatThreshold.value = it.learnIatUp
                ltftLearningGradientFactor.value = it.learnGrad
                ltftAbsGasPressureTreshold.value = it.learnGpa
                ltftDiffGasPressureTreshold.value = it.learnGpd
                ltftBottomLimitCorrection.value = it.min
                ltftTopLimitCorrection.value = it.max
                ltftLowerRpmThreshold.value = it.learnRpm0
                ltftUpperRpmThreshold.value = it.learnRpm1
                ltftLowerLoadThreshold.value = it.learnLoad0
                ltftUpperLoadThreshold.value = it.learnLoad1
                ltftNegativeInsensitivityThreshold.value = it.deadBand0
                ltftPositiveInsensitivityThreshold.value = it.deadBand1
                
            }

            initViews()

            mViewModel.isSendAllowed = true
        }
    }

    private fun initViews() {
        mBinding.apply {

            ltftMode.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.mode = position
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }


            ltftLowerCltThreshold.addOnValueChangeListener {
                packet?.learnClt = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftUpperCltThreshold.addOnValueChangeListener {
                packet?.learnCltUp = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftUpperIatThreshold.addOnValueChangeListener {
                packet?.learnIatUp = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftLearningGradientFactor.addOnValueChangeListener {
                packet?.learnGrad = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftAbsGasPressureTreshold.addOnValueChangeListener {
                packet?.learnGpa = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftDiffGasPressureTreshold.addOnValueChangeListener {
                packet?.learnGpd = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftBottomLimitCorrection.addOnValueChangeListener {
                packet?.min = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftTopLimitCorrection.addOnValueChangeListener {
                packet?.max = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftLowerRpmThreshold.addOnValueChangeListener {
                packet?.learnRpm0 = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftUpperRpmThreshold.addOnValueChangeListener {
                packet?.learnRpm1 = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftLowerLoadThreshold.addOnValueChangeListener {
                packet?.learnLoad0 = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftUpperLoadThreshold.addOnValueChangeListener {
                packet?.learnLoad1 = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftNegativeInsensitivityThreshold.addOnValueChangeListener {
                packet?.deadBand0 = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            ltftPositiveInsensitivityThreshold.addOnValueChangeListener {
                packet?.deadBand1 = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }

            ltftLowerCltThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftUpperCltThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftUpperIatThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftLearningGradientFactor.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftAbsGasPressureTreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftDiffGasPressureTreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftBottomLimitCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftTopLimitCorrection.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftLowerRpmThreshold.setOnClickListener { intParamClick(it as IntParamView) }
            ltftUpperRpmThreshold.setOnClickListener { intParamClick(it as IntParamView) }
            ltftLowerLoadThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftUpperLoadThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftNegativeInsensitivityThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }
            ltftPositiveInsensitivityThreshold.setOnClickListener { floatParamClick(it as FloatParamView) }

        }
    }

    override fun tabTitle(): Int {
        return R.string.params_tab_ltft_title
    }
}