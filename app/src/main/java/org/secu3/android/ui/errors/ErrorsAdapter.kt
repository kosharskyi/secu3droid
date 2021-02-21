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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import org.secu3.android.databinding.ViewErrorRowBinding
import org.secu3.android.models.CheckEngineError

class ErrorsAdapter(private val errors: List<CheckEngineError>) : RecyclerView.Adapter<ErrorsAdapter.ErrorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        val binding = ViewErrorRowBinding.inflate(layoutInflater, parent, false)
        return ErrorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ErrorViewHolder, position: Int) {
        holder.bind(errors[position])
    }

    override fun getItemCount(): Int {
        return errors.size
    }


    class ErrorViewHolder(private val binding: ViewErrorRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(error: CheckEngineError) {
            binding.errorName.text = error.title
            binding.errorIsActive.isInvisible = error.isActive.not()
            binding.errorIsSaved.isInvisible = error.isSaved.not()
        }
    }
}