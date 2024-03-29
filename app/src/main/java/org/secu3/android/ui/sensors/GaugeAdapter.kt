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

import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.secu3.android.R
import org.secu3.android.databinding.ViewGaugeItemBinding
import org.secu3.android.databinding.ViewNumericGaugeItemBinding
import org.secu3.android.db.models.GaugeState
import org.secu3.android.ui.sensors.models.GaugeItem
import org.secu3.android.ui.sensors.models.GaugeType
import java.util.Locale

class GaugeAdapter(val onSwitchViewClick: (GaugeState) -> Unit,
                   val onDeleteClick:(GaugeType) -> Unit) : ListAdapter<GaugeItem, ViewHolder>(GaugeItemDiffCallback) {

    private var mInflater: LayoutInflater? = null

    private val GAUGE_VIEW_TYPE = 0
    private val NUMERIC_VIEW_TYPE = 1

    override fun getItemViewType(position: Int): Int {

        val item = getItem(position)

        if (item.state.isNumericView) {
            return NUMERIC_VIEW_TYPE
        }

        return GAUGE_VIEW_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mInflater ?: let {
            mInflater = LayoutInflater.from(parent.context)
        }


        if (viewType == GAUGE_VIEW_TYPE) {
            val binding = ViewGaugeItemBinding.inflate(mInflater!!, parent, false)
            return GaugeViewHolder(binding)
        }

        val binding = ViewNumericGaugeItemBinding.inflate(mInflater!!, parent, false)

        return NumericGaugeViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as? GaugeViewHolder)?.bind(getItem(position))
        (holder as? NumericGaugeViewHolder)?.bind(getItem(position))
    }

    inner class GaugeViewHolder(private val binding: ViewGaugeItemBinding) : ViewHolder(binding.root) {

        private lateinit var gaugeType: GaugeType

        fun bind(item: GaugeItem) {
            gaugeType = item.state.gaugeType

            binding.apply {

                root.setOnClickListener {
                    val popup = PopupMenu(root.context, root)
                    val inflater: MenuInflater = popup.menuInflater
                    inflater.inflate(R.menu.gauge_actions_menu, popup.menu)
                    popup.show()
                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.switch_gauge_view -> {
                                onSwitchViewClick(item.state)
                                true
                            }
                            R.id.delete_gauge -> {
                                onDeleteClick(gaugeType)
                                true
                            }
                            else -> false
                        }
                    }
                }

                gaugeName.text = root.context.getString(gaugeType.title)

                gauge.apply {

                    setSpeedAt(item.value.toFloat())
                    unit = context.getString(gaugeType.units)
                    maxSpeed = 10_000f // to prevent exception if new min value is bigger than old max value
                    minSpeed = gaugeType.minValue
                    maxSpeed = gaugeType.maxValue
                    tickNumber = gaugeType.tickCount

                    val sections = gaugeType.getSections(context!!, speedometerWidth)
                    clearSections()
                    addSections(sections)

                    if (gaugeType.isInteger) {
                        speedTextListener = { speed -> speed.toInt().toString() }
                    } else {
                        speedTextListener = { speed -> "%.1f".format(Locale.US, speed) }
                    }
                }
            }

        }
    }

    inner class NumericGaugeViewHolder(private val binding: ViewNumericGaugeItemBinding) : ViewHolder(binding.root) {

        private lateinit var gaugeType: GaugeType

        fun bind(item: GaugeItem) {
            gaugeType = item.state.gaugeType

            binding.apply {

                root.setOnClickListener {
                    val popup = PopupMenu(root.context, root)
                    val inflater: MenuInflater = popup.menuInflater
                    inflater.inflate(R.menu.gauge_actions_menu, popup.menu)
                    popup.show()
                    popup.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.switch_gauge_view -> {
                                onSwitchViewClick(item.state)
                                true
                            }
                            R.id.delete_gauge -> {
                                onDeleteClick(gaugeType)
                                true
                            }
                            else -> false
                        }
                    }
                }

                gaugeName.text = root.context.getString(gaugeType.title)

                value.text = item.value

                units.text = root.context.getString(gaugeType.units)
            }

        }
    }

    object GaugeItemDiffCallback : DiffUtil.ItemCallback<GaugeItem>() {
        override fun areItemsTheSame(oldItem: GaugeItem, newItem: GaugeItem): Boolean {
            return oldItem.state == newItem.state
        }

        override fun areContentsTheSame(oldItem: GaugeItem, newItem: GaugeItem): Boolean {
            return oldItem.value == newItem.value
        }

        override fun getChangePayload(oldItem: GaugeItem, newItem: GaugeItem): Any {
            return newItem.value
        }
    }
}