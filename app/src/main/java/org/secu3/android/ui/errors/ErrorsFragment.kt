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

package org.secu3.android.ui.errors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentErrorsBinding
import org.secu3.android.models.CheckEngineError
import org.secu3.android.utils.Task

@AndroidEntryPoint
class ErrorsFragment : Fragment() {

    private lateinit var mBinding: FragmentErrorsBinding

    private val mViewModel: ErrorsViewModel by viewModels()

    private lateinit var errors: List<CheckEngineError>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentErrorsBinding.inflate(layoutInflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        errors = resources.getStringArray(R.array.errors_ecu_errors_names).map { CheckEngineError(it) }

        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            if (it) {
                mBinding.connectionStatus.text = getString(R.string.status_online)
            } else {
                mBinding.connectionStatus.text = getString(R.string.status_offline)
            }
        }

        mViewModel.checkEngineSavedLiveData.observe(viewLifecycleOwner) {

            for (i in errors.indices) {
                errors[i].isSaved = it.isError(i)
            }

            mBinding.errorsRecyclerView.adapter?.notifyDataSetChanged()
            mViewModel.sendNewTask(Task.Secu3ReadEcuErrors)
        }

        mViewModel.checkEngineLiveData.observe(viewLifecycleOwner) {
            for (i in errors.indices) {
                errors[i].isActive = it.isError(i)
            }

            mBinding.errorsRecyclerView.adapter?.notifyDataSetChanged()
        }

        mBinding.errorsRecyclerView.adapter = ErrorsAdapter(errors)

    }
}