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

package org.secu3.android.ui.sensors

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.secu3.android.R
import org.secu3.android.databinding.ViewIndicatorItemBinding
import org.secu3.android.ui.sensors.models.IndicatorItem
import org.secu3.android.ui.sensors.models.IndicatorType

class IndicatorAdapter(val onClick:(IndicatorType) -> Unit) : ListAdapter<IndicatorItem, IndicatorAdapter.IndicatorViewHolder>(IndicatorItemDiffCallback) {

    private var mInflater: LayoutInflater? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndicatorViewHolder {
        mInflater ?: let {
            mInflater = LayoutInflater.from(parent.context)
        }

        val binding = ViewIndicatorItemBinding.inflate(mInflater!!, parent, false)

        return IndicatorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IndicatorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IndicatorViewHolder(private val binding: ViewIndicatorItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IndicatorItem) {
            binding.apply {

                root.setOnClickListener {
                    val popup = PopupMenu(root.context, root)
                    val inflater: MenuInflater = popup.menuInflater
                    inflater.inflate(R.menu.indicator_actions_menu, popup.menu)
                    popup.show()
                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.delete_indicator -> {
                                onClick(item.type)
                                true
                            }
                            else -> false
                        }
                    }
                }

                indicatorName.text = root.context.getString(item.type.title)
                if (item.isActive) {
                    root.setCardBackgroundColor(Color.GREEN)
                } else {
                    root.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.colorPrimary))
                }
            }
        }
    }

    object IndicatorItemDiffCallback : DiffUtil.ItemCallback<IndicatorItem>() {
        override fun areItemsTheSame(oldItem: IndicatorItem, newItem: IndicatorItem): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: IndicatorItem, newItem: IndicatorItem): Boolean {
            return oldItem.isActive == newItem.isActive
        }
    }
}