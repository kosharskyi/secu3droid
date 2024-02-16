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

package org.secu3.android.ui.sensors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.secu3.android.R
import org.secu3.android.databinding.FragmentSensorsBinding
import org.secu3.android.ui.sensors.models.GaugeType
import org.secu3.android.ui.sensors.models.IndicatorType
import org.secu3.android.utils.Task


class SensorsFragment : Fragment() {

    private val mViewModel: SensorsViewModel by viewModels( ownerProducer = { requireParentFragment() } )

    private var mBinding: FragmentSensorsBinding? = null

    private var isDragging = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentSensorsBinding.inflate(inflater, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding?.apply {
            fab.setOnClickListener { onFabClick() }
            fabAddGauge.setOnClickListener {
                mViewModel.addGaugeClick()
                onFabClick()
            }
            fabAddIndicator.setOnClickListener {
                mViewModel.addIndicatorClick()
                onFabClick()
            }

            gaugeTouchHelper.attachToRecyclerView(gaugesList)
            gaugesList.adapter = GaugeAdapter(onSwitchViewClick = {
                it.isNumericView = it.isNumericView.not()
                mViewModel.updateGaugeState(it)
            }) {
                mViewModel.deleteGauge(it)
            }

            indicatorTouchHelper.attachToRecyclerView(indicatorsList)
            indicatorsList.adapter = IndicatorAdapter {
                mViewModel.deleteIndicator(it)
            }
        }


        mViewModel.gaugesLiveData.observe(viewLifecycleOwner) {
            if (isDragging) {
                return@observe
            }
            (mBinding?.gaugesList?.adapter as? GaugeAdapter)?.submitList(it)
        }

        mViewModel.indicatorLiveData.observe(viewLifecycleOwner) {
            if (isDragging) {
                return@observe
            }
            (mBinding?.indicatorsList?.adapter as? IndicatorAdapter)?.submitList(it)
        }

        mViewModel.showAddGaugeLiveData.observe(viewLifecycleOwner) {
            showGaugeSelectDialog(it)
        }

        mViewModel.showAddIndicatorLiveData.observe(viewLifecycleOwner) {
            showIndicatorSelectDialog(it)
        }
    }

    private fun onFabClick() {
        mBinding?.apply {
            if (fabAddGauge.isShown || fabAddIndicator.isShown) {
                fab.animate()
                    .rotation(0f)
                    .setDuration(500)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                disableFabs()
                return
            }

            fab.animate()
                .rotation(135f)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            enableFabs()
        }
    }

    private fun enableFabs() {
        mBinding?.apply {
            fabAddGauge.show()
            fabAddIndicator.show()
        }
    }

    private fun disableFabs() {
        mBinding?.apply {
            fabAddGauge.hide()
            fabAddIndicator.hide()
        }
    }

    private fun showGaugeSelectDialog(gauges: List<GaugeType>) {
        val gaugesNames = gauges.map { it.title }.map { getString(it) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_gauge_to_add))
            .setItems(gaugesNames) { dialog, which ->
                mViewModel.addGauge(gauges[which])
            }.show()
    }

    private fun showIndicatorSelectDialog(indicators: List<IndicatorType>) {
        val indicatorsNames = indicators.map { it.title }.map { getString(it) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_indicator_to_add))
            .setItems(indicatorsNames) { dialog, which ->
                mViewModel.addIndicator(indicators[which])
            }.show()
    }

    override fun onResume() {
        super.onResume()
        mViewModel.sendNewTask(Task.Secu3ReadSensors)

        mBinding?.apply {
            mViewModel.columnsCount.let {
                (gaugesList.layoutManager as GridLayoutManager).spanCount = it
                (indicatorsList.layoutManager as GridLayoutManager).spanCount = it
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

    private val gaugeTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.END or ItemTouchHelper.START
            )
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {

            val startIndex = viewHolder.adapterPosition
            val targetIndex = target.adapterPosition

            val adapter = mBinding?.gaugesList?.adapter as GaugeAdapter
            val listItems = adapter.currentList.toMutableList()

            val gauge = listItems[startIndex]
            listItems.removeAt(startIndex)
            listItems.add(targetIndex, gauge)

            val swipedItems = listItems.mapIndexed { index, gaugeItem ->
                gaugeItem.state.idx = index
                gaugeItem
            }

            adapter.submitList(swipedItems)
            mViewModel.gaugesSwiped(swipedItems.map { it.state })

            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // ignore
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            isDragging = false
        }

    })

    private val indicatorTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.END or ItemTouchHelper.START
            )
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val startIndex = viewHolder.adapterPosition
            val targetIndex = target.adapterPosition

            val adapter = mBinding?.indicatorsList?.adapter as IndicatorAdapter
            val listItems = adapter.currentList.toMutableList()

            val indicator = listItems[startIndex]
            listItems.removeAt(startIndex)
            listItems.add(targetIndex, indicator)

            val swipedItems = listItems.mapIndexed { index, indicatorItem ->
                indicatorItem.state.idx = index
                indicatorItem
            }

            adapter.submitList(swipedItems)
            mViewModel.indicatorsSwiped(swipedItems.map { it.state })
            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            isDragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // ignore
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            isDragging = false
        }

    })
}
