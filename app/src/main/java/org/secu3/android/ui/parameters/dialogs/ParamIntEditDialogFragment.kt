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
package org.secu3.android.ui.parameters.dialogs

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.math.BigInteger


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


    @Composable
    override fun DialogContent() {
        NumericEditDialogContent(
            title = paramTitle,
            initialText = currentValue.toString(),
            allowDecimal = false,
            allowNegative = minValue < 0,
            validate = { parseValidInput(it, minValue, maxValue) != null },
            onStep = { text, direction ->
                currentValue = stepValueFromInput(text, currentValue, stepValue, minValue, maxValue, direction)
                currentValue.toString()
            },
            onConfirm = { text, _ ->
                parseValidInput(text, minValue, maxValue)?.let { currentValue = it }
                _newValueLiveData.value = currentValue
                dismiss()
            },
            onCancel = { dismiss() },
        )
    }

    companion object {
        internal fun parseValidInput(value: String, min: Int, max: Int): Int? =
            value.toIntOrNull()?.takeIf { it in min..max }

        internal fun stepValueFromInput(
            value: String,
            current: Int,
            step: Int,
            min: Int,
            max: Int,
            direction: Int,
        ): Int {
            val base = value.toBigIntegerOrNull() ?: BigInteger.valueOf(current.toLong())
            val delta = BigInteger.valueOf(step.toLong()).multiply(BigInteger.valueOf(direction.toLong()))
            val stepped = base.add(delta)
            val bounded = stepped.max(BigInteger.valueOf(min.toLong())).min(BigInteger.valueOf(max.toLong()))
            return bounded.toInt()
        }

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
