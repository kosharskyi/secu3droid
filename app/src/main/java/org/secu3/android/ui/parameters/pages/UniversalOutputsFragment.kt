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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentUniversalOutputsBinding
import org.secu3.android.models.packets.params.UniOutParamPacket
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.utils.UnioutTabConfigurator
import javax.inject.Inject


@AndroidEntryPoint
class UniversalOutputsFragment : BaseParamFragment() {

    @Inject
    internal lateinit var mUnioutConfigurator: UnioutTabConfigurator

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentUniversalOutputsBinding

    private var packet: UniOutParamPacket? = null

    private val mCondition1List: List<String> by lazy {
        resources.getStringArray(R.array.uniout_spinner_conditions1).toList()
    }

    private val mCondition2List: List<String> by lazy {
        resources.getStringArray(R.array.uniout_spinner_conditions2).toList()
    }

    private val mLogicalFunctionsList: List<String> by lazy {
        resources.getStringArray(R.array.logical_functions).toList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentUniversalOutputsBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDropdowns()

        mViewModel.uniOutLiveData.observe(viewLifecycleOwner) {

            packet = it

            mBinding.apply {
                output1Condition1.setText(mCondition1List[it.output1Condition1], false)
                output1Condition1Inversion.apply {
                    isChecked = it.output1Cond1Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output1Cond1Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output1Condition1On.value = it.output1OnThrd1
                output1Condition1Off.value = it.output1OffThrd1

                it.output1LogicFunc.let { logicFunc ->
                    output1Condition2Group.isGone = logicFunc == 0xF

                    if (logicFunc == 0xF) {
                        output1LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output1LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                }

                output1Condition2.setText(mCondition1List[it.output1Condition2], false)
                output1Condition2Inversion.apply {
                    isChecked = it.output1Cond2Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output1Cond2Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output1Condition2On.value = it.output1OnThrd2
                output1Condition2Off.value = it.output1OffThrd2


                it.logicFunction_1_2.let { logicFunc ->
                    if (logicFunc == 0xF) {
                        logicalFunction12.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    logicalFunction12.setText(mLogicalFunctionsList[logicFunc], false)
                }


                output2Condition1.setText(mCondition1List[it.output2Condition1], false)
                output2Condition1Inversion.apply {
                    isChecked = it.output2Cond1Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output2Cond1Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output2Condition1On.value = it.output2OnThrd1
                output2Condition1Off.value = it.output2OffThrd1

                it.output2LogicFunc.let { logicFunc ->

                    output2Condition2Group.isGone = logicFunc == 0xF

                    if (logicFunc == 0xF) {
                        output2LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output2LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                }

                output2Condition2.setText(mCondition1List[it.output2Condition2], false)
                output2Condition2Inversion.apply {
                    isChecked = it.output2Cond2Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output2Cond2Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output2Condition2On.value = it.output2OnThrd2
                output2Condition2Off.value = it.output2OffThrd2


                output3Condition1.setText(mCondition1List[it.output3Condition1], false)
                output3Condition1Inversion.apply {
                    isChecked = it.output3Cond1Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output3Cond1Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output3Condition1On.value = it.output3OnThrd1
                output3Condition1Off.value = it.output3OffThrd1

                it.output3LogicFunc.let { logicFunc ->
                    output3Condition2Group.isGone = logicFunc == 0xF

                    if (logicFunc == 0xF) {
                        output3LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output3LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                }

                output3Condition2.setText(mCondition1List[it.output2Condition2], false)
                output3Condition2Inversion.apply {
                    isChecked = it.output3Cond2Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output3Cond2Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output3Condition2On.value = it.output3OnThrd2
                output3Condition2Off.value = it.output3OffThrd2



                output4Condition1.setText(mCondition1List[it.output4Condition1], false)
                output4Condition1Inversion.apply {
                    isChecked = it.output4Cond1Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output4Cond1Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output4Condition1On.value = it.output4OnThrd1
                output4Condition1Off.value = it.output4OffThrd1

                it.output4LogicFunc.let { logicFunc ->
                    output4Condition2Group.isGone = logicFunc == 0xF

                    if (logicFunc == 0xF) {
                        output4LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output4LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                }

                output4Condition2.setText(mCondition1List[it.output4Condition2], false)
                output4Condition2Inversion.apply {
                    isChecked = it.output4Cond2Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output4Cond2Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output4Condition2On.value = it.output4OnThrd2
                output4Condition2Off.value = it.output4OffThrd2



                output5Condition1.setText(mCondition1List[it.output5Condition1], false)
                output5Condition1Inversion.apply {
                    isChecked = it.output5Cond1Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output5Cond1Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output5Condition1On.value = it.output5OnThrd1
                output5Condition1Off.value = it.output5OffThrd1

                it.output5LogicFunc.let { logicFunc ->
                    output5Condition2Group.isGone = logicFunc == 0xF

                    if (logicFunc == 0xF) {
                        output5LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output5LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                }

                output5Condition2.setText(mCondition1List[it.output5Condition2], false)
                output5Condition2Inversion.apply {
                    isChecked = it.output5Cond2Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output5Cond2Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output5Condition2On.value = it.output5OnThrd2
                output5Condition2Off.value = it.output5OffThrd2



                output6Condition1.setText(mCondition1List[it.output6Condition1], false)
                output6Condition1Inversion.apply {
                    isChecked = it.output6Cond1Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output6Cond1Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output6Condition1On.value = it.output6OnThrd1
                output6Condition1Off.value = it.output6OffThrd1

                it.output6LogicFunc.let { logicFunc ->
                    output6Condition2Group.isGone = logicFunc == 0xF

                    if (logicFunc == 0xF) {
                        output6LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output6LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                }

                output6Condition2.setText(mCondition1List[it.output6Condition2], false)
                output6Condition2Inversion.apply {
                    isChecked = it.output6Cond2Inversion
                    setOnCheckedChangeListener { _, isChecked ->
                        it.output6Cond2Inversion = isChecked
                        mViewModel.sendPacket(it)
                    }
                }
                output6Condition2On.value = it.output6OnThrd2
                output6Condition2Off.value = it.output6OffThrd2

            }

            initViews()
        }
    }

    private fun initDropdowns() {
        mBinding.output1Condition1.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1List).also {
                setAdapter(it)
            }
        }

        mBinding.output1Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2List).also {
                setAdapter(it)
            }
        }

        mBinding.output1LogicalFunction.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mLogicalFunctionsList).also {
                setAdapter(it)
            }
        }




        mBinding.logicalFunction12.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mLogicalFunctionsList).also {
                setAdapter(it)
            }
        }




        mBinding.output2Condition1.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1List).also {
                setAdapter(it)
            }
        }

        mBinding.output2Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2List).also {
                setAdapter(it)
            }
        }

        mBinding.output2LogicalFunction.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mLogicalFunctionsList).also {
                setAdapter(it)
            }
        }




        mBinding.output3Condition1.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1List).also {
                setAdapter(it)
            }
        }

        mBinding.output3Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2List).also {
                setAdapter(it)
            }
        }

        mBinding.output3LogicalFunction.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mLogicalFunctionsList).also {
                setAdapter(it)
            }
        }




        mBinding.output4Condition1.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1List).also {
                setAdapter(it)
            }
        }

        mBinding.output4Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2List).also {
                setAdapter(it)
            }
        }

        mBinding.output4LogicalFunction.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mLogicalFunctionsList).also {
                setAdapter(it)
            }
        }




        mBinding.output5Condition1.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1List).also {
                setAdapter(it)
            }
        }

        mBinding.output5Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2List).also {
                setAdapter(it)
            }
        }

        mBinding.output5LogicalFunction.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mLogicalFunctionsList).also {
                setAdapter(it)
            }
        }




        mBinding.output6Condition1.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1List).also {
                setAdapter(it)
            }
        }

        mBinding.output6Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2List).also {
                setAdapter(it)
            }
        }

        mBinding.output6LogicalFunction.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mLogicalFunctionsList).also {
                setAdapter(it)
            }
        }
    }


    private fun initViews() {

        mBinding.apply {

            output1Condition1.setOnItemClickListener { _, _, position, _ ->
                packet?.output1Condition1 = position
                mUnioutConfigurator.configureViews(position, output1Condition1On, output1Condition1Off)
            }
            output1LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                if (position == 4) {
                    packet?.output1LogicFunc = 0xF
                } else {
                    packet?.output1LogicFunc = position
                }

                output1Condition2Group.isGone = position == 4

                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output1Condition2.setOnItemClickListener { _, _, position, _ ->
                packet?.output1Condition2 = position
                mUnioutConfigurator.configureViews(position, output1Condition2On, output1Condition2Off)
            }

            logicalFunction12.setOnItemClickListener { _, _, position, _ ->
                if (position == 4) {
                    packet?.logicFunction_1_2 = 0xF
                } else {
                    packet?.logicFunction_1_2 = position
                }
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            output2Condition1.setOnItemClickListener { _, _, position, _ ->
                packet?.output2Condition1 = position
                mUnioutConfigurator.configureViews(position, output2Condition1On, output2Condition1Off)
            }
            output2LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                if (position == 4) {
                    packet?.output2LogicFunc = 0xF
                } else {
                    packet?.output2LogicFunc = position
                }

                output2Condition2Group.isGone = position == 4

                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output2Condition2.setOnItemClickListener { _, _, position, _ ->
                packet?.output2Condition2 = position
                mUnioutConfigurator.configureViews(position, output2Condition2On, output2Condition2Off)
            }

            output3Condition1.setOnItemClickListener { _, _, position, _ ->
                packet?.output3Condition1 = position
                mUnioutConfigurator.configureViews(position, output3Condition1On, output3Condition1Off)
            }
            output3LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                if (position == 4) {
                    packet?.output3LogicFunc = 0xF
                } else {
                    packet?.output3LogicFunc = position
                }

                output3Condition2Group.isGone = position == 4

                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output3Condition2.setOnItemClickListener { _, _, position, _ ->
                packet?.output3Condition2 = position
                mUnioutConfigurator.configureViews(position, output3Condition2On, output3Condition2Off)
            }

            output4Condition1.setOnItemClickListener { _, _, position, _ ->
                packet?.output4Condition1 = position
                mUnioutConfigurator.configureViews(position, output4Condition1On, output4Condition1Off)
            }
            output4LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                if (position == 4) {
                    packet?.output4LogicFunc = 0xF
                } else {
                    packet?.output4LogicFunc = position
                }

                output4Condition2Group.isGone = position == 4

                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output4Condition2.setOnItemClickListener { _, _, position, _ ->
                packet?.output4Condition2 = position
                mUnioutConfigurator.configureViews(position, output4Condition2On, output4Condition2Off)
            }

            output5Condition1.setOnItemClickListener { _, _, position, _ ->
                packet?.output5Condition1 = position
                mUnioutConfigurator.configureViews(position, output5Condition1On, output5Condition1Off)
            }
            output5LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                if (position == 4) {
                    packet?.output5LogicFunc = 0xF
                } else {
                    packet?.output5LogicFunc = position
                }

                output5Condition2Group.isGone = position == 4

                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output5Condition2.setOnItemClickListener { _, _, position, _ ->
                packet?.output5Condition2 = position
                mUnioutConfigurator.configureViews(position, output5Condition2On, output5Condition2Off)
            }

            output6Condition1.setOnItemClickListener { _, _, position, _ ->
                packet?.output6Condition1 = position
                mUnioutConfigurator.configureViews(position, output6Condition1On, output6Condition1Off)
            }
            output6LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                if (position == 4) {
                    packet?.output6LogicFunc = 0xF
                } else {
                    packet?.output6LogicFunc = position
                }

                output6Condition2Group.isGone = position == 4

                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output6Condition2.setOnItemClickListener { _, _, position, _ ->
                packet?.output6Condition2 = position
                mUnioutConfigurator.configureViews(position, output6Condition2On, output6Condition2Off)
            }


            output1Condition1On.addOnValueChangeListener {
                packet?.output1OnThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output1Condition1Off.addOnValueChangeListener {
                packet?.output1OffThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            output1Condition2On.addOnValueChangeListener {
                packet?.output1OnThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output1Condition2Off.addOnValueChangeListener {
                packet?.output1OffThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }



            output2Condition1On.addOnValueChangeListener {
                packet?.output2OnThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output2Condition1Off.addOnValueChangeListener {
                packet?.output2OffThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            output2Condition2On.addOnValueChangeListener {
                packet?.output2OnThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output2Condition2Off.addOnValueChangeListener {
                packet?.output2OffThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }



            output3Condition1On.addOnValueChangeListener {
                packet?.output3OnThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output3Condition1Off.addOnValueChangeListener {
                packet?.output3OffThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            output3Condition2On.addOnValueChangeListener {
                packet?.output3OnThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output3Condition2Off.addOnValueChangeListener {
                packet?.output3OffThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }



            output4Condition1On.addOnValueChangeListener {
                packet?.output4OnThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output4Condition1Off.addOnValueChangeListener {
                packet?.output4OffThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            output4Condition2On.addOnValueChangeListener {
                packet?.output4OnThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output4Condition2Off.addOnValueChangeListener {
                packet?.output4OffThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }



            output5Condition1On.addOnValueChangeListener {
                packet?.output5OnThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output5Condition1Off.addOnValueChangeListener {
                packet?.output5OffThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            output5Condition2On.addOnValueChangeListener {
                packet?.output5OnThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output5Condition2Off.addOnValueChangeListener {
                packet?.output5OffThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }



            output6Condition1On.addOnValueChangeListener {
                packet?.output6OnThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output6Condition1Off.addOnValueChangeListener {
                packet?.output6OffThrd1 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }

            output6Condition2On.addOnValueChangeListener {
                packet?.output6OnThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }
            output6Condition2Off.addOnValueChangeListener {
                packet?.output6OffThrd2 = it
                packet?.let { it1 -> mViewModel.sendPacket(it1) }
            }


            output1Condition1On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output1Condition1Off.setOnClickListener { floatParamClick(it as FloatParamView) }
            output1Condition2On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output1Condition2Off.setOnClickListener { floatParamClick(it as FloatParamView) }

            output2Condition1On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output2Condition1Off.setOnClickListener { floatParamClick(it as FloatParamView) }
            output2Condition2On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output2Condition2Off.setOnClickListener { floatParamClick(it as FloatParamView) }

            output3Condition1On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output3Condition1Off.setOnClickListener { floatParamClick(it as FloatParamView) }
            output3Condition2On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output3Condition2Off.setOnClickListener { floatParamClick(it as FloatParamView) }

            output4Condition1On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output4Condition1Off.setOnClickListener { floatParamClick(it as FloatParamView) }
            output4Condition2On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output4Condition2Off.setOnClickListener { floatParamClick(it as FloatParamView) }

            output5Condition1On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output5Condition1Off.setOnClickListener { floatParamClick(it as FloatParamView) }
            output5Condition2On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output5Condition2Off.setOnClickListener { floatParamClick(it as FloatParamView) }

            output6Condition1On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output6Condition1Off.setOnClickListener { floatParamClick(it as FloatParamView) }
            output6Condition2On.setOnClickListener { floatParamClick(it as FloatParamView) }
            output6Condition2Off.setOnClickListener { floatParamClick(it as FloatParamView) }
        }
    }

}