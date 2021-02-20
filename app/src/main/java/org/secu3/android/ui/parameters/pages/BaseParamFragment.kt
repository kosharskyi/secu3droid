/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2021 Vitaliy O. Kosharskiy. Ukraine, Kharkiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv
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

import androidx.fragment.app.Fragment
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment
import org.secu3.android.ui.parameters.views.FloatParamView
import org.secu3.android.ui.parameters.views.IntParamView

abstract class BaseParamFragment : Fragment() {

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

}