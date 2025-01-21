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
package org.secu3.android.ui.diagnostics

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.databinding.FragmentDiagnosticsBinding

@AndroidEntryPoint
class DiagnosticsFragment : Fragment() {

    private val mViewModel: DiagnosticsViewModel by navGraphViewModels(R.id.diagnosticsFragment, factoryProducer = { defaultViewModelProviderFactory })

    private lateinit var mBinding: FragmentDiagnosticsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDiagnosticsBinding.inflate(layoutInflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            val color = when (it) {
                Connected -> {
                    ContextCompat.getColor(requireContext(), R.color.gauge_dark_green)
                }
                InProgress -> {
                    ContextCompat.getColor(requireContext(), R.color.gauge_dark_yellow)
                }
                Disconnected -> {
                    ContextCompat.getColor(requireContext(), R.color.gauge_red)
                }
                else -> {
                    return@observe
                }
            }

            mBinding.toolbar.menu?.findItem(R.id.connection_status)?.apply {
                val iconDrawable = icon ?: return@apply

                // Tint the icon with the desired color
                DrawableCompat.setTint(iconDrawable, color)
                DrawableCompat.setTintMode(iconDrawable, PorterDuff.Mode.SRC_IN)

                // Set the tinted icon to the menu item
                icon = iconDrawable
            }
        }

        mBinding.toolbar.apply {
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            inflateMenu(R.menu.fragment_diagnostics_menu)

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.diag_additional_settings -> {
                        findNavController().navigate(DiagnosticsFragmentDirections.actionDiagnosticToAdditional())
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }

        initPager()
    }

    private fun initPager() {
        mBinding.diagnosticsPager.adapter = DiagnosticsPagerAdapter(this)

        TabLayoutMediator(mBinding.tabLayout, mBinding.diagnosticsPager) { tab, position ->
            tab.text = resources.getStringArray(R.array.diagnostics_fragments_title)[position]
        }.attach()
    }

}