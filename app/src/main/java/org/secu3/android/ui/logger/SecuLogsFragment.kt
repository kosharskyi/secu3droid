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

package org.secu3.android.ui.logger

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSecuLogsBinding
import org.secu3.android.utils.gone
import org.secu3.android.utils.visible
import java.io.File


@AndroidEntryPoint
class SecuLogsFragment : Fragment() {

    internal var mBinding: FragmentSecuLogsBinding? = null

    private val mViewModel: SecuLogsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSecuLogsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding?.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            initList()
        }
    }

    private fun initList() {
        mBinding?.apply {
            mViewModel.getListOfLogFiles.takeIf { it.isNotEmpty() }?.let {
                emptyMessage.gone()
                recyclerView.visible()
                recyclerView.adapter = SecuLogsAdapter(it, { file ->
                    share(file)
                }) { file ->
                    confirmDeleteDialog(file)
                }
            } ?: let {
                recyclerView.gone()
                emptyMessage.visible()
            }
        }
    }

    private fun confirmDeleteDialog(file: File) {
        val show = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_confirm_delete_log_file_title))
            .setMessage(getString(R.string.dialog_confirm_delete_log_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                file.delete()
                initList()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .show()
    }

    private fun share(file: File) {
        val shareIntent: Intent = mViewModel.getShareIntent(file)
        startActivity(Intent.createChooser(shareIntent, null))
    }
}