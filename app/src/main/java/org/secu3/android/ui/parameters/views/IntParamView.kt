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

class IntParamView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var mBinding: ViewBaseParameterBinding

    private var mOnChangeValueListener: (Int) -> Unit = {}

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

    var value: Int = 0
        set(value) {
            field = value
            mBinding.paramValue.text = value.toString()
            mOnChangeValueListener(value)
        }

    var step: Int = 0
    var maxValue: Int = 0
    var minValue: Int = 0

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_base_parameter, this, true)
        mBinding = ViewBaseParameterBinding.bind(this)

        with(TypedValue()) {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
            setBackgroundResource(resourceId)
        }

        context.obtainStyledAttributes(attrs, R.styleable.IntParamView).apply {
            getString(R.styleable.IntParamView_title)?.let { title = it }
            getString(R.styleable.IntParamView_units)?.let { units = it }
            value = getInt(R.styleable.IntParamView_int_value, 0)
            step = getInt(R.styleable.IntParamView_int_step, 0)
            minValue = getInt(R.styleable.IntParamView_int_min_value, 0)
            maxValue = getInt(R.styleable.IntParamView_int_max_value, 0)

            recycle()
        }
    }

    fun addOnValueChangeListener(listener: (Int) -> Unit) {
        mOnChangeValueListener = listener
    }

}