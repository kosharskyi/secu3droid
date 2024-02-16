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

package org.secu3.android.ui.sensors.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSensorsSetingsBinding
import org.secu3.android.utils.UserPrefs
import javax.inject.Inject


@AndroidEntryPoint
class SensorsSettingsFragment : Fragment() {

    private var mBinding: FragmentSensorsSetingsBinding? = null

    @Inject
    internal lateinit var mPrefs: UserPrefs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsSetingsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding?.apply {

            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            oldSensorsCheckbox.isChecked = mPrefs.oldSensorViewEnabled
            oldSensorsCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
                mPrefs.oldSensorViewEnabled = isChecked
            }

            oldSensorsTitle.setOnClickListener {
                oldSensorsCheckbox.performClick()
            }

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}