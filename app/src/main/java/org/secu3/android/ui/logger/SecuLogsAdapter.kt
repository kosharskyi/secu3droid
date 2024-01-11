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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.secu3.android.databinding.ItemSecuLogBinding
import org.secu3.android.utils.sizeStr
import java.io.File

class SecuLogsAdapter(private val files: List<File>, val shareClick: (File) -> Unit, val deleteClick: (File) -> Unit) : RecyclerView.Adapter<SecuLogsAdapter.SecuLogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SecuLogViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = ItemSecuLogBinding.inflate(inflater, parent, false)

        return SecuLogViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onBindViewHolder(holder: SecuLogViewHolder, position: Int) {
        holder.bind(files[position])
    }

    inner class SecuLogViewHolder(private val binding: ItemSecuLogBinding) : ViewHolder(binding.root) {

        private var file: File? = null

        fun bind(file: File) {
            this.file = file

            binding.apply {
                fileName.text = file.name
                fileSize.text = file.sizeStr()
                shareBtn.setOnClickListener {
                    shareClick(file)
                }
                deleteBtn.setOnClickListener {
                    deleteClick(file)
                }
            }
        }
    }
}