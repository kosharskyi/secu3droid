package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.secu3.android.R
import org.secu3.android.databinding.FragmentDbwBinding
import org.secu3.android.models.packets.out.params.DbwParamPacket
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible

class DbwFragment : BaseParamFragment() {

    private var binding: FragmentDbwBinding? = null

    private var packet: DbwParamPacket? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDbwBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.dbwLiveData.observe(viewLifecycleOwner) {

            mViewModel.isSendAllowed = false

            packet = it

            binding?.apply {

                progressBar.gone()
                params.visible()

                etcP.value = it.etc_p
                etcI.value = it.etc_i
                etcD.value = it.etc_d
                nmaxDuty.value = it.etc_nmax_duty
                pmaxDuty.value = it.etc_pmax_duty
                pidPeriod.value = it.pid_period
                frictionTorqueOpen.value = it.frictorq_open
                frictionTorqueClose.value = it.frictorq_close
                frictionThrd.value = it.frictorq_thrd
                frictorqIddleAddMax.value = it.frictorq_idleadd_max
            }

            initViews()

            mViewModel.isSendAllowed = true
        }
    }

    private fun initViews() {

        binding?.apply {
            etcP.addOnValueChangeListener {
                packet?.etc_p = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            etcI.addOnValueChangeListener {
                packet?.etc_i = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            etcD.addOnValueChangeListener {
                packet?.etc_d = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            nmaxDuty.addOnValueChangeListener {
                packet?.etc_nmax_duty = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            pmaxDuty.addOnValueChangeListener {
                packet?.etc_pmax_duty = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            pidPeriod.addOnValueChangeListener {
                packet?.pid_period = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            frictionTorqueOpen.addOnValueChangeListener {
                packet?.frictorq_open = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            frictionTorqueClose.addOnValueChangeListener {
                packet?.frictorq_close = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            frictionThrd.addOnValueChangeListener {
                packet?.frictorq_thrd = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }
            frictorqIddleAddMax.addOnValueChangeListener {
                packet?.frictorq_idleadd_max = it
                packet?.let { pckt -> mViewModel.sendPacket(pckt) }
            }


            etcP.setOnClickListener { floatParamClick(it as FloatParamView) }
            etcI.setOnClickListener { floatParamClick(it as FloatParamView) }
            etcD.setOnClickListener { floatParamClick(it as FloatParamView) }
            nmaxDuty.setOnClickListener { floatParamClick(it as FloatParamView) }
            pmaxDuty.setOnClickListener { floatParamClick(it as FloatParamView) }
            pidPeriod.setOnClickListener { floatParamClick(it as FloatParamView) }
            frictionTorqueOpen.setOnClickListener { floatParamClick(it as FloatParamView) }
            frictionTorqueClose.setOnClickListener { floatParamClick(it as FloatParamView) }
            frictionThrd.setOnClickListener { floatParamClick(it as FloatParamView) }
            frictorqIddleAddMax.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }

    override fun tabTitle(): Int {
        return R.string.params_tab_el_throttle_title
    }
}