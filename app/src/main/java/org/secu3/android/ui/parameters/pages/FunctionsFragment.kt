/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentFunctionsBinding
import org.secu3.android.ui.parameters.ParamsViewModel
import kotlin.experimental.and


class FunctionsFragment : Fragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentFunctionsBinding

    private val loadMeasurementItems = listOf("MAP", "MAP(baro)", "TPS", "MAP+TPS")
    private val mapselItems = mapOf(1 to "1", 2 to "2", 3 to "3", 15 to "no")
    private val barocorrItems = listOf("Disabled", "Static MAP1", "Dynamic MAP1", "Dynamic MAP2")

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
                val adapter = ArrayAdapter(requireContext(), R.layout.list_item, mapsSetList )
                mapsSet.setAdapter(adapter)

                mapsSetForGas.inputType = InputType.TYPE_NULL
                val adapterGas = ArrayAdapter(requireContext(), R.layout.list_item, mapsSetList )
                mapsSetForGas.setAdapter(adapterGas)
            }
        }

        mViewModel.funsetLiveData.observe(viewLifecycleOwner) {

            mBinding.apply {

                mViewModel.mFnNameDatPacket?.fnNameList?.map { fn -> fn.name }?.let { fnNames ->
                    mapsSet.setText(fnNames[it.fnGasoline], false)
                    mapsSetForGas.setText(fnNames[it.fnGas], false)
                }

                lowerLoadValue.value = it.loadLower
                upperLoadValue.value = it.loadUpper

                mapCurveOffset.value = it.mapCurveOffset
                mapCurveGradient.value = it.mapCurveGradient

                tpsCurveOffset.value = it.tpsCurveOffset
                tpsCurveGradient.value = it.tpsCurveGradient


                loadMeasurement.setText(loadMeasurementItems[it.loadSrcCfg], false)

                mapselPetrol.setText(mapselItems[it.mapserUniPetrol], false)
                mapselGas.setText(mapselItems[it.mapserUniGas], false)

                barometricCorrection.setText(barocorrItems[it.barocorrType], false)

                map2CurveOffset.value = it.map2CurveOffset
                map2CurveGradient.value = it.map2CurveGradient
            }
        }
    }

    private fun init() {
        initLoadMeasurement()
        initMapsel()
        initBarometricCorrection()
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
}