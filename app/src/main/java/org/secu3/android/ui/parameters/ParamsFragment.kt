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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.ui.parameters.dialogs.ParamFloatEditDialogFragment
import org.secu3.android.ui.parameters.dialogs.ParamIntEditDialogFragment

@AndroidEntryPoint
class ParamsFragment : Fragment() {
    private val mViewModel: ParamsViewModel by viewModels()

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val pages = parameterPageSpecs(mViewModel.fwInfoPacket)
        val pageTitles = pages.map { it.titleRes }
        return ComposeView(requireContext()).apply {
            id = R.id.params_view_container
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val connectionState by mViewModel.connectionStatusLiveData.observeAsState()
                ParamsScreen(
                    titles = pageTitles.map { stringResource(it) },
                    pageKeys = pageTitles,
                    connectionState = connectionState,
                    onBack = { findNavController().navigateUp() },
                    onSave = mViewModel::savePacket,
                    onPageSettled = { index -> pageTitles.getOrNull(index)?.let(mViewModel::activatePage) },
                    onPageUnsettled = mViewModel::deactivatePage,
                    pageContent = { index, active, isActiveNow -> pageTitles.getOrNull(index)?.let { title ->
                        ParameterPageContent(title, mViewModel, active, isActiveNow) { showEditRequest(it) }
                    } },
                )
            }
        }
    }

    private fun showEditRequest(request: ParameterEditRequest) {
        when (request) {
            is ParameterEditRequest.IntValue -> ParamIntEditDialogFragment.newInstance(
                request.value, request.title, request.step, request.max, request.min,
            ).also { dialog ->
                val observer = object : Observer<Int> {
                    override fun onChanged(value: Int) {
                        dialog.newValueLiveData.removeObserver(this)
                        request.onValue(value)
                    }
                }
                dialog.newValueLiveData.observe(viewLifecycleOwner, observer)
                dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
            is ParameterEditRequest.FloatValue -> ParamFloatEditDialogFragment.newInstance(
                request.value, request.title, request.step, request.max, request.min, request.precision,
            ).also { dialog ->
                val observer = object : Observer<Float> {
                    override fun onChanged(value: Float) {
                        dialog.newValueLiveData.removeObserver(this)
                        request.onValue(value)
                    }
                }
                dialog.newValueLiveData.observe(viewLifecycleOwner, observer)
                dialog.show(childFragmentManager, dialog::class.java.simpleName)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.connectionStatusLiveData.observe(viewLifecycleOwner) {
            if (!mViewModel.isConnected) mViewModel.clearPageState()
        }
    }

    override fun onPause() {
        mViewModel.deactivatePage()
        mViewModel.cancelManualCommands()
        super.onPause()
    }

    override fun onDestroyView() {
        mViewModel.clearPageState()
        super.onDestroyView()
    }
}

@Composable
internal fun ParamsScreen(
    titles: List<String>,
    pageKeys: List<Int> = titles.indices.toList(),
    connectionState: org.secu3.android.connection.ConnectionState?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPageSettled: (Int) -> Unit,
    pageContent: @Composable (Int, Boolean, () -> Boolean) -> Unit,
    onPageUnsettled: () -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { titles.size })
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var hostResumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            hostResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var pagerSize by remember { mutableStateOf(IntSize.Zero) }
    val selectedIndex = clampPageIndex(pagerState.settledPage, titles.size)
    val updatedSettled by rememberUpdatedState(onPageSettled)
    val updatedUnsettled by rememberUpdatedState(onPageUnsettled)
    val updatedConnectionState by rememberUpdatedState(connectionState)
    val tabBringIntoView = remember(titles) { titles.map { BringIntoViewRequester() } }

    LaunchedEffect(selectedIndex, tabBringIntoView) { tabBringIntoView.getOrNull(selectedIndex)?.bringIntoView() }
    LaunchedEffect(lifecycle, pagerState, titles.size) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var activeIndex: Int? = null
            try {
                snapshotFlow {
                    val index = pagerState.settledPage
                    Triple(index, pagerState.isScrollInProgress,
                        pagerSize.width > 0 && pagerSize.height > 0 && updatedConnectionState == Connected &&
                            pagerState.layoutInfo.visiblePagesInfo.any { it.index == index })
                }.distinctUntilChanged().collect { (index, scrolling, laidOutVisible) ->
                    val next = index.takeIf { !scrolling && laidOutVisible && it in titles.indices }
                    if (next != activeIndex) {
                        if (activeIndex != null) updatedUnsettled()
                        activeIndex = next
                        if (next != null) updatedSettled(next)
                    }
                }
            } finally {
                if (activeIndex != null) updatedUnsettled()
            }
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).background(colorResource(R.color.colorPrimary))) {
        ParamsToolbar(connectionState, onBack, onSave)
        Row(Modifier.fillMaxWidth().height(48.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            titles.forEachIndexed { index, title ->
                val selected = selectedIndex == index
                Column(
                    Modifier.height(48.dp).bringIntoViewRequester(tabBringIntoView[index])
                        .semantics { role = Role.Tab; this.selected = selected; contentDescription = title }
                        .clickable(role = Role.Tab) { scope.launch { pagerState.animateScrollToPage(index) } }
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(title, color = if (selected) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selected) Surface(Modifier.fillMaxWidth().padding(top = 5.dp).height(2.dp), color = MaterialTheme.colors.secondary) {}
                }
            }
        }
        HorizontalPager(
            state = pagerState,
            key = { index -> pageKeys.getOrElse(index) { index } },
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("params-pager").onSizeChanged { pagerSize = it },
        ) { index ->
            val active = hostResumed && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && index == selectedIndex &&
                !pagerState.isScrollInProgress && pagerSize != IntSize.Zero && updatedConnectionState == Connected &&
                pagerState.layoutInfo.visiblePagesInfo.any { it.index == index }
            val isActiveNow = {
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && pagerState.settledPage == index &&
                    !pagerState.isScrollInProgress && pagerSize != IntSize.Zero && updatedConnectionState == Connected &&
                    pagerState.layoutInfo.visiblePagesInfo.any { it.index == index }
            }
            pageContent(index, active, isActiveNow)
        }
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
