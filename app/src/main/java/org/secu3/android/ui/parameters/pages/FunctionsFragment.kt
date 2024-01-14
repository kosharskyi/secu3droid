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
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentFunctionsBinding
import org.secu3.android.models.packets.params.FunSetParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView


class FunctionsFragment : BaseParamFragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentFunctionsBinding

    private var packet: FunSetParamPacket? = null

    private val loadMeasurementItems : List<String> by lazy {
        resources.getStringArray(R.array.load_measurement_items).toList()
    }
    private val mapselItems = mapOf(1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5", 6 to "6", 15 to "no")
    private val barocorrItems : List<String> by lazy {
        resources.getStringArray(R.array.barocorr_items).toList()
    }

    private val ve2MapFuncItems : List<String> by lazy {
        resources.getStringArray(R.array.ve2_map_func_items).toList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentFunctionsBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        mViewModel.fnNameLiveData.observe(viewLifecycleOwner) {
            mBinding.apply {

                val mapsSetList = it.fnNameList.map { it.name }

                mapsSet.inputType = InputType.TYPE_NULL
                val adapter = ArrayAdapter(requireContext(), R.layout.list_item, mapsSetList)
                mapsSet.setAdapter(adapter)

                mapsSetForGas.inputType = InputType.TYPE_NULL
                val adapterGas = ArrayAdapter(requireContext(), R.layout.list_item, mapsSetList)
                mapsSetForGas.setAdapter(adapterGas)
            }
        }

        mViewModel.funsetLiveData.observe(viewLifecycleOwner) {

            packet = it

            mBinding.apply {

                mViewModel.mFnNameDatPacket?.fnNameList?.map { fn -> fn.name }?.let { fnNames ->
                    mapsSet.setText(fnNames[it.fnGasoline], false)
                    mapsSetForGas.setText(fnNames[it.fnGas], false)
                }

                lowerLoadValue.value = it.loadLower
                upperLoadValue.value = it.loadUpper
                useLoadGrid.isChecked = it.useLoadGrid

                mapCurveOffset.value = it.mapCurveOffset
                mapCurveGradient.value = it.mapCurveGradient

                tpsCurveOffset.value = it.tpsCurveOffset
                tpsCurveGradient.value = it.tpsCurveGradient


                loadMeasurement.setText(loadMeasurementItems[it.loadSrcCfg], false)

                mapselPetrol.setText(mapselItems[it.mapselUniPetrol], false)
                mapselGas.setText(mapselItems[it.mapselUniGas], false)

                barometricCorrection.setText(barocorrItems[it.barocorrType], false)

                map2CurveOffset.value = it.map2CurveOffset
                map2CurveGradient.value = it.map2CurveGradient

                ve2MapFunc.setText(ve2MapFuncItems[it.ve2MapFunc], false)
                gasVCondition.setText(mapselItems[it.gasVUni], false)
            }

            initViews()
        }
    }

    private fun init() {
        initLoadMeasurement()
        initMapsel()
        initBarometricCorrection()

        mBinding.apply {
            ve2MapFunc.inputType = InputType.TYPE_NULL
            val adapter = ArrayAdapter(requireContext(), R.layout.list_item, ve2MapFuncItems)
            ve2MapFunc.setAdapter(adapter)

            gasVCondition.inputType = InputType.TYPE_NULL
            val gasAdapter = ArrayAdapter(requireContext(), R.layout.list_item, mapselItems.values.toList())
            gasVCondition.setAdapter(gasAdapter)
        }
    }

    private fun initLoadMeasurement() {
        mBinding.loadMeasurement.inputType = InputType.TYPE_NULL
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, loadMeasurementItems)
        mBinding.loadMeasurement.setAdapter(adapter)
    }

    private fun initMapsel() {
        mBinding.apply {
            mapselGas.inputType = InputType.TYPE_NULL
            mapselPetrol.inputType = InputType.TYPE_NULL

            val gasAdapter = ArrayAdapter(requireContext(), R.layout.list_item, mapselItems.values.toList())
            mapselGas.setAdapter(gasAdapter)

            val petrolAdapter = ArrayAdapter(requireContext(), R.layout.list_item, mapselItems.values.toList())
            mapselPetrol.setAdapter(petrolAdapter)
        }
    }

    private fun initBarometricCorrection() {
        mBinding.barometricCorrection.inputType = InputType.TYPE_NULL
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, barocorrItems)
        mBinding.barometricCorrection.setAdapter(adapter)
    }

    private fun initViews() {

        mBinding.apply {
            lowerLoadValue.addOnValueChangeListener {
                packet?.loadLower = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            upperLoadValue.addOnValueChangeListener {
                packet?.loadUpper = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            useLoadGrid.setOnCheckedChangeListener { buttonView, isChecked ->
                packet?.apply {
                    useLoadGrid = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            mapCurveOffset.addOnValueChangeListener {
                packet?.mapCurveOffset = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            mapCurveGradient.addOnValueChangeListener {
                packet?.mapCurveGradient = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            tpsCurveOffset.addOnValueChangeListener {
                packet?.tpsCurveOffset = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            tpsCurveGradient.addOnValueChangeListener {
                packet?.tpsCurveGradient = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            map2CurveOffset.addOnValueChangeListener {
                packet?.map2CurveOffset = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            map2CurveGradient.addOnValueChangeListener {
                packet?.map2CurveGradient = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            mapsSet.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.fnGasoline = position
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            mapsSetForGas.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.fnGas = position
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            loadMeasurement.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.loadSrcCfg = position
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            mapselPetrol.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.mapselUniPetrol = mapselItems.keys.elementAt(position)
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            mapselGas.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.mapselUniGas = mapselItems.keys.elementAt(position)
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            barometricCorrection.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.barocorrType = position
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            ve2MapFunc.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    ve2MapFunc = position
                    mViewModel.sendPacket(this)
                }
            }

            gasVCondition.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    gasVUni = mapselItems.keys.elementAt(position)
                    mViewModel.sendPacket(this)
                }
            }


            lowerLoadValue.setOnClickListener { floatParamClick(it as FloatParamView) }
            upperLoadValue.setOnClickListener { floatParamClick(it as FloatParamView) }
            mapCurveOffset.setOnClickListener { floatParamClick(it as FloatParamView) }
            mapCurveGradient.setOnClickListener { floatParamClick(it as FloatParamView) }
            tpsCurveOffset.setOnClickListener { floatParamClick(it as FloatParamView) }
            tpsCurveGradient.setOnClickListener { floatParamClick(it as FloatParamView) }
            map2CurveOffset.setOnClickListener { floatParamClick(it as FloatParamView) }
            map2CurveGradient.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }
}