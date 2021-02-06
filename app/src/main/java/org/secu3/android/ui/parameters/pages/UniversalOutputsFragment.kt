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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import org.secu3.android.R
import org.secu3.android.databinding.FragmentUniversalOutputsBinding
import org.secu3.android.ui.parameters.ParamsViewModel


class UniversalOutputsFragment : Fragment() {

    private val mViewModel: ParamsViewModel by activityViewModels()
    private lateinit var mBinding: FragmentUniversalOutputsBinding

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
            mBinding.apply {
                output1Condition1.setText(mCondition1List[it.output1Condition1], false)
                output1Condition1Inversion.isChecked = it.output1Cond1Inversion
                output1Condition1On.value = it.output1OnThrd1
                output1Condition1Off.value = it.output1OffThrd1

                it.output1LogicFunc.let {
                    if (it == 15) {
                        output1LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output1LogicalFunction.setText(mLogicalFunctionsList[it], false)
                }

                output1Condition2.setText(mCondition1List[it.output1Condition2], false)
                output1Condition2Inversion.isChecked = it.output1Cond2Inversion
                output1Condition2On.value = it.output1OnThrd2
                output1Condition2Off.value = it.output1OffThrd2


                it.logicFunction_1_2.let {
                    if (it == 15) {
                        logicalFunction12.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    logicalFunction12.setText(mLogicalFunctionsList[it], false)
                }


                output2Condition1.setText(mCondition1List[it.output2Condition1], false)
                output2Condition1Inversion.isChecked = it.output2Cond1Inversion
                output2Condition1On.value = it.output2OnThrd1
                output2Condition1Off.value = it.output2OffThrd1

                it.output2LogicFunc.let {
                    if (it == 15) {
                        output2LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output2LogicalFunction.setText(mLogicalFunctionsList[it], false)
                }

                output2Condition2.setText(mCondition1List[it.output2Condition2], false)
                output2Condition2Inversion.isChecked = it.output2Cond2Inversion
                output2Condition2On.value = it.output2OnThrd2
                output2Condition2Off.value = it.output2OffThrd2


                output3Condition1.setText(mCondition1List[it.output3Condition1], false)
                output3Condition1Inversion.isChecked = it.output3Cond1Inversion
                output3Condition1On.value = it.output3OnThrd1
                output3Condition1Off.value = it.output3OffThrd1

                it.output3LogicFunc.let {
                    if (it == 15) {
                        output3LogicalFunction.setText(mLogicalFunctionsList[4], false)
                        return@let
                    }
                    output3LogicalFunction.setText(mLogicalFunctionsList[it], false)
                }

                output3Condition2.setText(mCondition1List[it.output2Condition2], false)
                output3Condition2Inversion.isChecked = it.output3Cond2Inversion
                output3Condition2On.value = it.output3OnThrd2
                output3Condition2Off.value = it.output3OffThrd2

            }
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
    }
}