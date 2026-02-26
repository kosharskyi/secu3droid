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

package org.secu3.android.ui.parameters.pages

import android.view.MotionEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import org.secu3.android.ui.parameters.ParamsViewModel
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView

abstract class BaseParamFragment : Fragment() {

    protected val mViewModel: ParamsViewModel by viewModels( ownerProducer = { requireParentFragment() })

    protected fun intParamClick(view: IntParamView) {
        view.apply {
            ParamIntEditDialogFragment.newInstance(value, title, step, maxValue, minValue).also {
                it.newValueLiveData.observe(viewLifecycleOwner) { result ->
                    value = result
                }

                it.show(childFragmentManager, it::class.java.simpleName)
            }
        }
    }

    // NOTE: make sure you've changed this method in [DiagInputFragment.kt]
    protected fun floatParamClick(view: FloatParamView) {
        view.apply {
            ParamFloatEditDialogFragment.newInstance(value, title, step, maxValue, minValue, precision).also {
                it.newValueLiveData.observe(viewLifecycleOwner) { result ->
                    value = result
                }

                it.show(childFragmentManager, it::class.java.simpleName)
            }
        }
    }

    protected fun MaterialButton.setPressAndHoldRepeater(
        initialDelayMs: Long = 400L,
        repeatDelayMs: Long = 80L,
        fireImmediately: Boolean = true,
        onTick: () -> Unit
    ) {
        var isDown = false
        var didRepeat = false

        val repeatRunnable = object : Runnable {
            override fun run() {
                if (!isDown) return
                didRepeat = true
                onTick()
                postDelayed(this, repeatDelayMs)
            }
        }

        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isDown = true
                    didRepeat = false
                    v.isPressed = true

                    if (fireImmediately) {
                        onTick()
                    }

                    v.postDelayed(repeatRunnable, initialDelayMs)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    isDown = false
                    v.isPressed = false
                    v.removeCallbacks(repeatRunnable)

                    // Якщо не було повторів і ми не робили immediate tick — це звичайний tap
                    if (!didRepeat && !fireImmediately) onTick()

                    // І ОТУТ — єдиний тригер "відправити накопичене"
                    v.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    isDown = false
                    v.isPressed = false
                    v.removeCallbacks(repeatRunnable)
                    true
                }

                else -> false
            }
        }
    }

    abstract fun tabTitle(): Int

}