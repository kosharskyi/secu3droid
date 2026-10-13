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
package org.secu3.android.ui.parameters

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress

@AndroidEntryPoint
class ParamsFragment : Fragment() {

    private val mViewModel: ParamsViewModel by viewModels()
    private var settledPageTitleRes: Int? = null

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val firmwareInfo = mViewModel.fwInfoPacket
        val pageSpecs = parameterPageSpecs(firmwareInfo)
        return ComposeView(requireContext()).apply {
            id = R.id.params_view_container
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val connectionState by mViewModel.connectionStatusLiveData.observeAsState()
                val titles = remember(firmwareInfo) { pageSpecs.map { getString(it.titleRes) } }
                ParamsScreen(
                    titles = titles,
                    connectionState = connectionState,
                    onBack = { findNavController().navigateUp() },
                    onSave = { mViewModel.savePacket() },
                    onPageSettled = { index ->
                        settledPageTitleRes = pageSpecs.getOrNull(index)?.titleRes
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            settledPageTitleRes?.let(mViewModel::activatePage)
                        }
                    },
                    onPageUnsettled = {
                        settledPageTitleRes = null
                        mViewModel.deactivatePage()
                    },
                    createPager = { context ->
                        ViewPager2(context).apply {
                            id = R.id.paramsPager
                            adapter = ParametersPagerAdapter(this@ParamsFragment, firmwareInfo)
                            offscreenPageLimit = 2
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settledPageTitleRes?.let(mViewModel::activatePage)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            if (!mViewModel.isConnected) {
                mViewModel.clearPageState()
            } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                settledPageTitleRes?.let(mViewModel::activatePage)
            }
        }
    }

    override fun onPause() {
        mViewModel.deactivatePage()
        super.onPause()
    }

    override fun onDestroyView() {
        mViewModel.clearPageState()
        settledPageTitleRes = null
        super.onDestroyView()
    }
}

@Composable
internal fun ParamsScreen(
    titles: List<String>,
    connectionState: org.secu3.android.connection.ConnectionState?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPageSettled: (Int) -> Unit,
    createPager: (android.content.Context) -> ViewPager2,
    onPageUnsettled: () -> Unit = {},
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var pager by remember { mutableStateOf<ViewPager2?>(null) }
    val safeIndex = clampPageIndex(selectedIndex, titles.size)
    val latestSelection by rememberUpdatedState(safeIndex)
    val latestOnPageSettled by rememberUpdatedState(onPageSettled)
    val latestOnPageUnsettled by rememberUpdatedState(onPageUnsettled)
    val tabBringIntoView = remember(titles) { titles.map { BringIntoViewRequester() } }

    LaunchedEffect(safeIndex, tabBringIntoView) {
        tabBringIntoView.getOrNull(safeIndex)?.bringIntoView()
    }

    DisposableEffect(pager, titles.size) {
        val viewPager = pager
        if (viewPager == null) return@DisposableEffect onDispose { }
        var disposed = false
        val listener = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectedIndex = clampPageIndex(position, titles.size)
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE && viewPager.isLaidOut) {
                    latestOnPageSettled(clampPageIndex(viewPager.currentItem, titles.size))
                } else if (state != ViewPager2.SCROLL_STATE_IDLE) {
                    latestOnPageUnsettled()
                }
            }
        }
        viewPager.registerOnPageChangeCallback(listener)
        viewPager.setCurrentItem(latestSelection, false)
        viewPager.doOnPreDraw {
            if (!disposed && viewPager.scrollState == ViewPager2.SCROLL_STATE_IDLE) {
                latestOnPageSettled(clampPageIndex(viewPager.currentItem, titles.size))
            }
        }
        onDispose {
            disposed = true
            viewPager.unregisterOnPageChangeCallback(listener)
        }
    }

    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(colorResource(R.color.colorPrimary))
    ) {
        ParamsToolbar(connectionState, onBack, onSave)
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            titles.forEachIndexed { index, title ->
                val selected = safeIndex == index
                Column(
                    modifier = Modifier.height(48.dp)
                        .bringIntoViewRequester(tabBringIntoView[index])
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                            contentDescription = title
                        }
                        .clickable(role = Role.Tab) { selectedIndex = index; pager?.setCurrentItem(index, true) }
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        color = if (selected) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (selected) Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(2.dp),
                        color = MaterialTheme.colors.secondary,
                    ) {}
                }
            }
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("params-pager"),
            factory = { context -> createPager(context).also { pager = it } },
        )
    }
}

@Composable
private fun ParamsToolbar(connectionState: org.secu3.android.connection.ConnectionState?, onBack: () -> Unit, onSave: () -> Unit) {
    val barColor = colorResource(R.color.colorPrimary)
    val statusLabel = when (connectionState) {
        Connected -> stringResource(R.string.home_connection_connected)
        InProgress -> stringResource(R.string.home_connection_connecting)
        Disconnected -> stringResource(R.string.home_connection_disconnected)
        else -> stringResource(R.string.connection_status_unknown)
    }
    val statusColor = when (connectionState) {
        Connected -> colorResource(R.color.gauge_dark_green)
        InProgress -> colorResource(R.color.gauge_dark_yellow)
        Disconnected -> colorResource(R.color.gauge_red)
        else -> null
    }
    val saveDescription = stringResource(R.string.save)
    val backDescription = stringResource(R.string.back)
    Surface(color = barColor, elevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backDescription }) {
                Icon(painterResource(R.drawable.ic_arrow_back_24), contentDescription = null, tint = MaterialTheme.colors.onSurface)
            }
            Text(
                text = stringResource(R.string.title_activity_param),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1,
            )
            IconButton(onClick = onSave, modifier = Modifier.semantics { contentDescription = saveDescription }) {
                Icon(painterResource(R.drawable.ic_save_24), contentDescription = null, tint = MaterialTheme.colors.onSurface)
            }
            Row(modifier = Modifier.semantics { contentDescription = statusLabel }, verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_car),
                    contentDescription = null,
                    tint = statusColor ?: MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
                )
            }
        }
    }
}

internal fun clampPageIndex(index: Int, pageCount: Int): Int = if (pageCount <= 0) 0 else index.coerceIn(0, pageCount - 1)
