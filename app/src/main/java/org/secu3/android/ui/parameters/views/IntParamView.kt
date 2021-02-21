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