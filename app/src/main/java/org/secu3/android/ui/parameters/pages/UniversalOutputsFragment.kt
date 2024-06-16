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
package org.secu3.android.ui.parameters.pages

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.databinding.FragmentUniversalOutputsBinding
import org.secu3.android.models.packets.out.params.UniOutParamPacket
import org.secu3.android.models.packets.out.params.UniOutParamPacket.*
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.utils.UnioutTabConfigurator
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible
import javax.inject.Inject


@AndroidEntryPoint
class UniversalOutputsFragment : BaseParamFragment() {

    @Inject
    internal lateinit var mUnioutConfigurator: UnioutTabConfigurator

    private lateinit var mBinding: FragmentUniversalOutputsBinding

    private var packet: UniOutParamPacket? = null

    private val mCondition1List: List<CONDITION> by lazy {
        UniOutParamPacket.CONDITION.entries.filter { it.condition1 }
    }

    private val mCondition2List: List<CONDITION> by lazy {
        UniOutParamPacket.CONDITION.entries.filter { it.condition2 }
    }

    private val mCondition1ListText: List<String> by lazy {
        mCondition1List.map { getString(it.strId) }
    }

    private val mCondition2ListText: List<String> by lazy {
        mCondition2List.map { getString(it.strId) }
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

        initViews()

        lifecycleScope.launch {
            withResumed {
                mViewModel.uniOutLiveData.observe(viewLifecycleOwner) {

                    mViewModel.isSendAllowed = false

                    packet = it

                    mBinding.apply {
                        progressBar.gone()
                        params.visible()

                        output1Use.isChecked = it.output1Use
                        output1Group.isVisible = it.output1Use

                        output2Use.isChecked = it.output2Use
                        output2Group.isVisible = it.output2Use

                        output3Use.isChecked = it.output3Use
                        output3Group.isVisible = it.output3Use

                        output4Use.isChecked = it.output4Use
                        output4Group.isVisible = it.output4Use

                        output5Use.isChecked = it.output5Use
                        output5Group.isVisible = it.output5Use

                        output6Use.isChecked = it.output6Use
                        output6Group.isVisible = it.output6Use

                        mCondition1List.first { condition -> condition.id == it.output1Condition1 }.let { condition ->
                            output1Condition1.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output1Condition1On,
                                output1Condition1Off,
                                it.output1OnThrd1,
                                it.output1OffThrd1)
                        }

                        output1Condition1Inversion.isChecked = it.output1Cond1Inversion
                        output1Inversion.isChecked = it.output1Inversion

                        it.output1LogicFunc.let { logicFunc ->
                            if (packet?.output1Use == true) {
                                output1Condition2Group.isGone = logicFunc == 0xF
                            }

                            if (logicFunc == 0xF) {
                                output1LogicalFunction.setText(mLogicalFunctionsList[4], false)
                                return@let
                            }
                            output1LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                        }

                        mCondition2List.first { condition -> condition.id == it.output1Condition2 }.let { condition ->
                            output1Condition2.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output1Condition2On,
                                output1Condition2Off,
                                it.output1OnThrd2,
                                it.output1OffThrd2
                                )
                        }

                        output1Condition2Inversion.isChecked = it.output1Cond2Inversion


                        it.logicFunction_1_2.let { logicFunc ->
                            if (logicFunc == 0xF) {
                                logicalFunction12.setText(mLogicalFunctionsList[4], false)
                                return@let
                            }
                            logicalFunction12.setText(mLogicalFunctionsList[logicFunc], false)
                        }


                        mCondition1List.first { condition -> condition.id == it.output2Condition1 }.let { condition ->
                            output2Condition1.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output2Condition1On,
                                output2Condition1Off,
                                it.output2OnThrd1,
                                it.output2OffThrd1
                                )
                        }
                        output2Condition1Inversion.isChecked = it.output2Cond1Inversion
                        output2Inversion.isChecked = it.output2Inversion

                        it.output2LogicFunc.let { logicFunc ->
                            if (packet?.output2Use == true) {
                                output2Condition2Group.isGone = logicFunc == 0xF
                            }


                            if (logicFunc == 0xF) {
                                output2LogicalFunction.setText(mLogicalFunctionsList[4], false)
                                return@let
                            }
                            output2LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                        }

                        mCondition2List.first { condition -> condition.id == it.output2Condition2 }.let { condition ->
                            output2Condition2.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output2Condition2On,
                                output2Condition2Off,
                                it.output2OnThrd2,
                                it.output2OffThrd2
                                )
                        }
                        output2Condition2Inversion.isChecked = it.output2Cond2Inversion


                        mCondition1List.first { condition -> condition.id == it.output3Condition1 }.let { condition ->
                            output3Condition1.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output3Condition1On,
                                output3Condition1Off,
                                it.output3OnThrd1,
                                it.output3OffThrd1
                                )
                        }
                        output3Condition1Inversion.isChecked = it.output3Cond1Inversion
                        output3Inversion.isChecked = it.output3Inversion

                        it.output3LogicFunc.let { logicFunc ->
                            if (packet?.output3Use == true) {
                                output3Condition2Group.isGone = logicFunc == 0xF
                            }

                            if (logicFunc == 0xF) {
                                output3LogicalFunction.setText(mLogicalFunctionsList[4], false)
                                return@let
                            }
                            output3LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                        }

                        mCondition2List.first { condition -> condition.id == it.output3Condition2 }.let { condition ->
                            output3Condition2.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output3Condition2On,
                                output3Condition2Off,
                                it.output3OnThrd2,
                                it.output3OffThrd2
                                )
                        }
                        output3Condition2Inversion.isChecked = it.output3Cond2Inversion



                        mCondition1List.first { condition -> condition.id == it.output4Condition1 }.let { condition ->
                            output4Condition1.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output4Condition1On,
                                output4Condition1Off,
                                it.output4OnThrd1,
                                it.output4OffThrd1
                                )
                        }
                        output4Condition1Inversion.isChecked = it.output4Cond1Inversion
                        output4Inversion.isChecked = it.output4Inversion

                        it.output4LogicFunc.let { logicFunc ->
                            if (packet?.output4Use == true) {
                                output4Condition2Group.isGone = logicFunc == 0xF
                            }

                            if (logicFunc == 0xF) {
                                output4LogicalFunction.setText(mLogicalFunctionsList[4], false)
                                return@let
                            }
                            output4LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                        }

                        mCondition2List.first { condition -> condition.id == it.output4Condition2 }.let { condition ->
                            output4Condition2.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output4Condition2On,
                                output4Condition2Off,
                                it.output4OnThrd2,
                                it.output4OffThrd2
                                )
                        }
                        output4Condition2Inversion.isChecked = it.output4Cond2Inversion



                        mCondition1List.first { condition -> condition.id == it.output5Condition1 }.let { condition ->
                            output5Condition1.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output5Condition1On,
                                output5Condition1Off,
                                it.output5OnThrd1,
                                it.output5OffThrd1
                                )
                        }
                        output5Condition1Inversion.isChecked = it.output5Cond1Inversion
                        output5Inversion.isChecked = it.output5Inversion

                        it.output5LogicFunc.let { logicFunc ->
                            if (packet?.output5Use == true) {
                                output5Condition2Group.isGone = logicFunc == 0xF
                            }

                            if (logicFunc == 0xF) {
                                output5LogicalFunction.setText(mLogicalFunctionsList[4], false)
                                return@let
                            }
                            output5LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                        }

                        mCondition2List.first { condition -> condition.id == it.output5Condition2 }.let { condition ->
                            output5Condition2.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output5Condition2On,
                                output5Condition2Off,
                                it.output5OnThrd2,
                                it.output5OffThrd2
                                )
                        }
                        output5Condition2Inversion.isChecked = it.output5Cond2Inversion



                        mCondition1List.first { condition -> condition.id == it.output6Condition1 }.let { condition ->
                            output6Condition1.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output6Condition1On,
                                output6Condition1Off,
                                it.output6OnThrd1,
                                it.output6OffThrd1
                                )
                        }
                        output6Condition1Inversion.isChecked = it.output6Cond1Inversion
                        output6Inversion.isChecked = it.output6Inversion

                        it.output6LogicFunc.let { logicFunc ->
                            if (packet?.output6Use == true) {
                                output6Condition2Group.isGone = logicFunc == 0xF
                            }

                            if (logicFunc == 0xF) {
                                output6LogicalFunction.setText(mLogicalFunctionsList[4], false)
                                return@let
                            }
                            output6LogicalFunction.setText(mLogicalFunctionsList[logicFunc], false)
                        }

                        mCondition2List.first { condition -> condition.id == it.output6Condition2 }.let { condition ->
                            output6Condition2.setText(getString(condition.strId), false)
                            mUnioutConfigurator.configureViews(
                                condition,
                                output6Condition2On,
                                output6Condition2Off,
                                it.output6OnThrd2,
                                it.output6OffThrd2
                                )
                        }
                        output6Condition2Inversion.isChecked = it.output6Cond2Inversion
                    }

                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun initDropdowns() {
        mBinding.output1Condition1.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1ListText).also {
                setAdapter(it)
            }
        }

        mBinding.output1Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2ListText).also {
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
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1ListText).also {
                setAdapter(it)
            }
        }

        mBinding.output2Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2ListText).also {
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
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1ListText).also {
                setAdapter(it)
            }
        }

        mBinding.output3Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2ListText).also {
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
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1ListText).also {
                setAdapter(it)
            }
        }

        mBinding.output4Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2ListText).also {
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
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1ListText).also {
                setAdapter(it)
            }
        }

        mBinding.output5Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2ListText).also {
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
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition1ListText).also {
                setAdapter(it)
            }
        }

        mBinding.output6Condition2.apply {
            inputType = InputType.TYPE_NULL
            ArrayAdapter(requireContext(), R.layout.list_item, mCondition2ListText).also {
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

            output1Condition1Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output1Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }
            output1Condition2Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output1Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output2Condition1Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output2Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }
            output2Condition2Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output2Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output3Condition1Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output3Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }
            output3Condition2Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output3Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output4Condition1Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output4Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }
            output4Condition2Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output4Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output5Condition1Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output5Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }
            output5Condition2Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output5Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output6Condition1Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output6Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }
            output6Condition2Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output6Cond1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }


            output1Use.setOnCheckedChangeListener { _, isChecked ->
                output1Group.isVisible = isChecked
                packet?.apply {
                    output1Use = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output2Use.setOnCheckedChangeListener { _, isChecked ->
                output2Group.isVisible = isChecked
                packet?.apply {
                    output2Use = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output3Use.setOnCheckedChangeListener { _, isChecked ->
                output3Group.isVisible = isChecked
                packet?.apply {
                    output3Use = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output4Use.setOnCheckedChangeListener { _, isChecked ->
                output4Group.isVisible = isChecked
                packet?.apply {
                    output4Use = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output5Use.setOnCheckedChangeListener { _, isChecked ->
                output5Group.isVisible = isChecked
                packet?.apply {
                    output5Use = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output6Use.setOnCheckedChangeListener { _, isChecked ->
                output6Group.isVisible = isChecked
                packet?.apply {
                    output6Use = isChecked
                    mViewModel.sendPacket(this)
                }
            }



            output1Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output1Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output2Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output2Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output3Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output3Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output4Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output4Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output5Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output5Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }

            output6Inversion.setOnCheckedChangeListener { _, isChecked ->
                packet?.apply {
                    output6Inversion = isChecked
                    mViewModel.sendPacket(this)
                }
            }



            output1Condition1.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition1List[position]
                packet?.output1Condition1 = condition.id
                mUnioutConfigurator.configureViews(condition, output1Condition1On, output1Condition1Off)
            }
            output1LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    if (position == 4) {
                        output1LogicFunc = 0xF
                    } else {
                        output1LogicFunc = position
                    }

                    output1Condition2Group.isGone = position == 4

                    mViewModel.sendPacket(this)
                }
            }
            output1Condition2.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition2List[position]
                packet?.output1Condition2 = condition.id
                mUnioutConfigurator.configureViews(condition, output1Condition2On, output1Condition2Off)
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
                val condition = mCondition1List[position]
                packet?.output2Condition1 = condition.id
                mUnioutConfigurator.configureViews(condition, output2Condition1On, output2Condition1Off)
            }
            output2LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    if (output2Use.not()) {
                        return@setOnItemClickListener
                    }

                    if (position == 4) {
                        output2LogicFunc = 0xF
                    } else {
                        output2LogicFunc = position
                    }

                    output2Condition2Group.isGone = position == 4

                    mViewModel.sendPacket(this)
                }
            }
            output2Condition2.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition2List[position]
                packet?.output2Condition2 = condition.id
                mUnioutConfigurator.configureViews(condition, output2Condition2On, output2Condition2Off)
            }

            output3Condition1.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition1List[position]
                packet?.output3Condition1 = condition.id
                mUnioutConfigurator.configureViews(condition, output3Condition1On, output3Condition1Off)
            }
            output3LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    if (position == 4) {
                        output3LogicFunc = 0xF
                    } else {
                        output3LogicFunc = position
                    }

                    output3Condition2Group.isGone = position == 4

                    mViewModel.sendPacket(this)
                }
            }
            output3Condition2.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition2List[position]
                packet?.output3Condition2 = condition.id
                mUnioutConfigurator.configureViews(condition, output3Condition2On, output3Condition2Off)
            }

            output4Condition1.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition1List[position]
                packet?.output4Condition1 = condition.id
                mUnioutConfigurator.configureViews(condition, output4Condition1On, output4Condition1Off)
            }
            output4LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    if (position == 4) {
                        output4LogicFunc = 0xF
                    } else {
                        output4LogicFunc = position
                    }

                    output4Condition2Group.isGone = position == 4

                    mViewModel.sendPacket(this)
                }
            }
            output4Condition2.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition2List[position]
                packet?.output4Condition2 = condition.id
                mUnioutConfigurator.configureViews(condition, output4Condition2On, output4Condition2Off)
            }

            output5Condition1.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition1List[position]
                packet?.output5Condition1 = condition.id
                mUnioutConfigurator.configureViews(condition, output5Condition1On, output5Condition1Off)
            }
            output5LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    if (position == 4) {
                        output5LogicFunc = 0xF
                    } else {
                        output5LogicFunc = position
                    }

                    output5Condition2Group.isGone = position == 4

                    mViewModel.sendPacket(this)
                }
            }
            output5Condition2.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition2List[position]
                packet?.output5Condition2 = condition.id
                mUnioutConfigurator.configureViews(condition, output5Condition2On, output5Condition2Off)
            }

            output6Condition1.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition1List[position]
                packet?.output6Condition1 = condition.id
                mUnioutConfigurator.configureViews(condition, output6Condition1On, output6Condition1Off)
            }
            output6LogicalFunction.setOnItemClickListener { _, _, position, _ ->
                packet?.apply {
                    if (position == 4) {
                        output6LogicFunc = 0xF
                    } else {
                        output6LogicFunc = position
                    }

                    output6Condition2Group.isGone = position == 4

                    mViewModel.sendPacket(this)
                }
            }
            output6Condition2.setOnItemClickListener { _, _, position, _ ->
                val condition = mCondition2List[position]
                packet?.output6Condition2 = condition.id
                mUnioutConfigurator.configureViews(condition, output6Condition2On, output6Condition2Off)
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