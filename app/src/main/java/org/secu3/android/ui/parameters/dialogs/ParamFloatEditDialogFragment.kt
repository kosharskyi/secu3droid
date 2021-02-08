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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.secu3.android.R
import org.secu3.android.databinding.FragmentParamsBinding
import org.secu3.android.databinding.FragmentParamsEditDialogBinding



class ParamFloatEditDialogFragment : ParamBaseEditDialogFragment() {

    private var currentValue: Float = 0f
    private var paramTitle: String = ""
    private var stepValue: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentValue = it.getFloat(ARG_CURRENT_VALUE)
            paramTitle = it.getString(ARG_PARAM_TITLE, "")
            stepValue = it.getFloat(ARG_STEP_VALUE)
        }
    }


    private val _newValueLiveData = MutableLiveData<Float>()
    val newValueLiveData: LiveData<Float>
        get() = _newValueLiveData


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            parameterTitle.text = paramTitle
            value.text = "%.2f".format(currentValue)


            increment.setOnClickListener {
                currentValue = currentValue.plus(stepValue)
                value.text = "%.2f".format(currentValue)
            }
            decrement.setOnClickListener {
                currentValue = currentValue.minus(stepValue)
                value.text = "%.2f".format(currentValue)
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
        fun newInstance(currentValue: Float, paramTitle: String, stepValue: Float) = ParamFloatEditDialogFragment().apply {
            arguments = Bundle().apply {
                putFloat(ARG_CURRENT_VALUE, currentValue)
                putString(ARG_PARAM_TITLE, paramTitle)
                putFloat(ARG_STEP_VALUE, stepValue)
            }
        }
    }
}