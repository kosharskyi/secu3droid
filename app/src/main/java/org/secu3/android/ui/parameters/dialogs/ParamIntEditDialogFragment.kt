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
package org.secu3.android.ui.parameters.dialogs

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class ParamIntEditDialogFragment : ParamBaseEditDialogFragment() {

    private var currentValue: Int = 0
    private var paramTitle: String = ""
    private var stepValue: Int = 0
    private var maxValue: Int = 0
    private var minValue: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentValue = it.getInt(ARG_CURRENT_VALUE)
            paramTitle = it.getString(ARG_PARAM_TITLE, "")
            stepValue = it.getInt(ARG_STEP_VALUE)
            maxValue = it.getInt(ARG_MAX_VALUE)
            minValue = it.getInt(ARG_MIN_VALUE)
        }
    }


    private val _newValueLiveData = MutableLiveData<Int>()
    val newValueLiveData: LiveData<Int>
        get() = _newValueLiveData


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            parameterTitle.text = paramTitle
            value.text = currentValue.toString()


            increment.setOnClickListener {
                var resValue = currentValue.plus(stepValue)

                if (resValue > maxValue) {
                    resValue = maxValue
                }

                currentValue = resValue
                value.text = currentValue.toString()
            }
            decrement.setOnClickListener {
                var resValue = currentValue.minus(stepValue)

                if (resValue < minValue) {
                    resValue = minValue
                }

                currentValue = resValue
                value.text = currentValue.toString()
            }

            ok.setOnClickListener {
                _newValueLiveData.value = currentValue
                dismiss()
            }
            cancel.setOnClickListener { dismiss() }

        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param currentValue Parameter 1.
         * @param paramTitle Parameter 2.
         * @param stepValue Parameter 3.
         * @return A new instance of fragment ParamsEditDialogFragment.
         */
        fun newInstance(currentValue: Int, paramTitle: String, stepValue: Int, maxValue: Int, minValue: Int) = ParamIntEditDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_CURRENT_VALUE, currentValue)
                putString(ARG_PARAM_TITLE, paramTitle)
                putInt(ARG_STEP_VALUE, stepValue)
                putInt(ARG_MAX_VALUE, maxValue)
                putInt(ARG_MIN_VALUE, minValue)
            }
        }
    }
}