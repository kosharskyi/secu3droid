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
package org.secu3.android.ui.parameters.dialogs

import android.text.InputType
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Locale


class ParamFloatEditDialogFragment : ParamBaseEditDialogFragment() {

    private var currentValue: Float = 0f
    private var paramTitle: String = ""
    private var stepValue: Float = 0f
    private var maxValue: Float = 0f
    private var minValue: Float = 0f
    private var precision: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentValue = it.getFloat(ARG_CURRENT_VALUE)
            paramTitle = it.getString(ARG_PARAM_TITLE, "")
            stepValue = it.getFloat(ARG_STEP_VALUE)
            maxValue = it.getFloat(ARG_MAX_VALUE)
            minValue = it.getFloat(ARG_MIN_VALUE)
            precision = it.getInt(ARG_PRECISION)
        }
    }


    private val _newValueLiveData = MutableLiveData<Float>()
    val newValueLiveData: LiveData<Float>
        get() = _newValueLiveData


    @Composable
    override fun DialogContent() {
        NumericEditDialogContent(
            title = paramTitle,
            initialText = formatStr.format(Locale.US, currentValue),
            allowDecimal = true,
            allowNegative = minValue < 0f,
            validate = { isValidInput(it, minValue, maxValue) },
            onStep = { text, direction ->
                currentValue = stepValueFromInput(text, currentValue, stepValue, minValue, maxValue, direction)
                formatStr.format(Locale.US, currentValue)
            },
            onConfirm = { text, changed ->
                if (changed && isValidInput(text, minValue, maxValue)) currentValue = text.toFloat()
                _newValueLiveData.value = currentValue
                dismiss()
            },
            onCancel = { dismiss() },
        )
    }

    private val formatStr: String
        get() {
            return when (precision) {
                0 -> "%.0f"
                1 -> "%.1f"
                2 -> "%.2f"
                3 -> "%.3f"
                4 -> "%.4f"
                else -> "%.4f"
            }
        }

    companion object {

        internal fun isValidInput(value: String, min: Float, max: Float): Boolean =
            value.toFloatOrNull()?.let { it in min..max } == true

        internal fun stepValueFromInput(
            value: String,
            current: Float,
            step: Float,
            min: Float,
            max: Float,
            direction: Int,
        ): Float {
            val base = value.toFloatOrNull()?.takeUnless { it.isNaN() } ?: current
            return (base + step * direction).coerceIn(min, max)
        }

        internal fun inputTypeFor(min: Float): Int = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or
            if (min < 0f) InputType.TYPE_NUMBER_FLAG_SIGNED else 0

        private const val ARG_PRECISION = "precision"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param currentValue Parameter 1.
         * @param paramTitle Parameter 2.
         * @param stepValue Parameter 3.
         * @return A new instance of fragment ParamsEditDialogFragment.
         */
        fun newInstance(currentValue: Float, paramTitle: String, stepValue: Float,
                        maxValue: Float, minValue: Float, precision: Int) = ParamFloatEditDialogFragment().apply {
            arguments = Bundle().apply {
                putFloat(ARG_CURRENT_VALUE, currentValue)
                putString(ARG_PARAM_TITLE, paramTitle)
                putFloat(ARG_STEP_VALUE, stepValue)
                putFloat(ARG_MAX_VALUE, maxValue)
                putFloat(ARG_MIN_VALUE, minValue)
                putInt(ARG_PRECISION, precision)
            }
        }
    }
}
