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

package org.secu3.android.ui.parameters.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.secu3.android.R
import org.secu3.android.databinding.ViewBaseParameterBinding

class FloatParamView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var mBinding: ViewBaseParameterBinding

    private var mOnChangeValueListener: (Float) -> Unit = {}

    var title: String = ""
        set(value) {
            field = value
            mBinding.paramTitle.text = value
        }

    var units: String = ""
        set(value) {
            field = value
            mBinding.paramUnits.text = value
        }

    var value: Float = 0f
        set(value) {
            field = value
            showValue()
            mOnChangeValueListener(value)
        }

    var step: Float = 0f
    var maxValue: Float = 0f
    var minValue: Float = 0f

    var precision: Int = 0

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_base_parameter, this, true)
        mBinding = ViewBaseParameterBinding.bind(this)

        with(TypedValue()) {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
            setBackgroundResource(resourceId)
        }

        context.obtainStyledAttributes(attrs, R.styleable.FloatParamView).apply {
            getString(R.styleable.FloatParamView_title)?.let { title = it }
            getString(R.styleable.FloatParamView_units)?.let { units = it }
            value = getFloat(R.styleable.FloatParamView_float_value, 0f)
            step = getFloat(R.styleable.FloatParamView_float_step, 0f)
            maxValue = getFloat(R.styleable.FloatParamView_float_max_value, 0f)
            minValue = getFloat(R.styleable.FloatParamView_float_min_value, 0f)
            precision = getInt(R.styleable.FloatParamView_value_precision, 0)

            recycle()
        }
    }

    private fun showValue() {
        mBinding.paramValue.text = when (precision) {
            0 -> "%.0f".format(value)
            1 -> "%.1f".format(value)
            2 -> "%.2f".format(value)
            3 -> "%.3f".format(value)
            4 -> "%.4f".format(value)
            5 -> "%.5f".format(value)
            else -> value.toString()
        }
    }

    fun addOnValueChangeListener(listener: (Float) -> Unit) {
        mOnChangeValueListener = listener
    }
}