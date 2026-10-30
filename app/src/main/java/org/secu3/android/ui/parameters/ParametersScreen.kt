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
 */
package org.secu3.android.ui.parameters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.SecuTopAppBar
import org.secu3.android.ui.compose.SecuTabRow
import org.secu3.android.ui.parameters.dialogs.NumericEditDialog

@Composable
internal fun ParametersRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: ParamsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val capabilities by viewModel.capabilities.collectAsStateWithLifecycle()
    val pages = remember(capabilities) { viewModel.parameterPages(capabilities) }
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle(initialValue = null)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.pausePageSession()
                viewModel.cancelManualCommands()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (activity.isChangingConfigurations) {
                viewModel.deactivatePage()
                viewModel.cancelManualCommands()
            } else {
                viewModel.clearPageState()
            }
        }
    }
    SecuComposeTheme {
        ParametersScreen(
            titles = pages.map { stringResource(it.titleRes) },
            pageKeys = pages.map { it.titleRes },
            connectionState = connectionState,
            connectionSession = viewModel.connectionSession,
            editorSession = viewModel.parameterEditSession,
            onEditConfirm = viewModel::acceptParameterEditor,
            onEditDismiss = viewModel::dismissParameterEditor,
            onBack = onNavigateUp,
            onSave = viewModel::savePacket,
            onPageSettled = { index -> pages.getOrNull(index)?.page?.let(viewModel::activatePage) },
            onPageUnsettled = viewModel::deactivatePage,
            pageContent = { index, active, isActiveNow -> pages.getOrNull(index)?.let { page ->
                ParameterPageContent(page.page, viewModel, capabilities, active, isActiveNow, viewModel::openParameterEditor)
            } },
        )
    }
}

@Composable
internal fun ParametersScreen(
    titles: List<String>,
    pageKeys: List<Int> = titles.indices.toList(),
    connectionState: org.secu3.android.connection.ConnectionState?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPageSettled: (Int) -> Unit,
    pageContent: @Composable (Int, Boolean, () -> Boolean) -> Unit,
    editorSession: ParameterEditSession? = null,
    onEditConfirm: (Long, Number) -> Unit = { _, _ -> },
    onEditDismiss: (Long) -> Unit = {},
    onPageUnsettled: () -> Unit = {},
    connectionSession: Int = 0,
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
    val settledIndex = clampPageIndex(pagerState.settledPage, titles.size)
    val updatedSettled by rememberUpdatedState(onPageSettled)
    val updatedUnsettled by rememberUpdatedState(onPageUnsettled)
    val updatedConnectionState by rememberUpdatedState(connectionState)
    LaunchedEffect(lifecycle, pagerState, titles.size, connectionSession) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var activeIndex: Int? = null
            try {
                snapshotFlow {
                    val index = pagerState.settledPage
                    Triple(index, pagerState.isScrollInProgress,
                        pagerSize.width > 0 && pagerSize.height > 0 && updatedConnectionState == Connected &&
                            pagerState.layoutInfo.visiblePagesInfo.any { it.index == index })
                }.distinctUntilChanged().collect { (index, scrolling, laidOutVisible) ->
                    val next = when {
                        updatedConnectionState != Connected -> null
                        scrolling -> activeIndex
                        laidOutVisible -> index.takeIf { it in titles.indices }
                        else -> null
                    }
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

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .background(MaterialTheme.colorScheme.background),
    ) {
        ParametersToolbar(connectionState, onBack, onSave)
        SecuTabRow(
            titles = titles,
            selectedIndex = clampPageIndex(pagerState.currentPage, titles.size),
            selectionPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
            onSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
            scrollable = true,
            testTag = "parameters-tabs",
        )
        HorizontalPager(
            state = pagerState,
            key = { index -> pageKeys.getOrElse(index) { index } },
            beyondViewportPageCount = 1,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("params-pager").onSizeChanged { pagerSize = it },
        ) { index ->
            val rendered = remember(index) { mutableStateOf(false) }
            val readyToRender by remember(index, pagerState) {
                derivedStateOf {
                    rendered.value || !pagerState.isScrollInProgress ||
                        index in minOf(pagerState.currentPage, pagerState.targetPage)..maxOf(pagerState.currentPage, pagerState.targetPage) ||
                        pagerState.layoutInfo.visiblePagesInfo.any { it.index == index }
                }
            }
            if (updatedConnectionState == Connected && readyToRender && !rendered.value) {
                SideEffect { rendered.value = true }
            }
            val active = hostResumed && index == settledIndex &&
                !pagerState.isScrollInProgress && pagerSize != IntSize.Zero && updatedConnectionState == Connected &&
                pagerState.layoutInfo.visiblePagesInfo.any { it.index == index }
            val isActiveNow = {
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && pagerState.settledPage == index &&
                    !pagerState.isScrollInProgress && pagerSize != IntSize.Zero && updatedConnectionState == Connected &&
                    pagerState.layoutInfo.visiblePagesInfo.any { it.index == index }
            }
            if (updatedConnectionState == Connected && readyToRender) {
                pageContent(index, active, isActiveNow)
            } else if (updatedConnectionState == Connected) {
                Box(Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().testTag("parameters-unavailable"), contentAlignment = Alignment.Center) {
                    Text(stringResource(if (updatedConnectionState == InProgress) R.string.home_connection_connecting else R.string.home_connection_disconnected))
                }
            }
        }
    }
    editorSession?.takeIf { connectionState == Connected }?.let { session ->
        NumericEditDialog(
            request = session.request,
            sessionId = session.id,
            onConfirm = { onEditConfirm(session.id, it) },
            onDismiss = { onEditDismiss(session.id) },
        )
    }
}

@Composable
private fun ParametersToolbar(connectionState: org.secu3.android.connection.ConnectionState?, onBack: () -> Unit, onSave: () -> Unit) {
    SecuTopAppBar(
        title = stringResource(R.string.title_activity_param),
        connectionState = connectionState,
        onNavigateUp = onBack,
        showStatus = true,
        actions = {
            IconButton(onClick = onSave, enabled = connectionState == Connected) {
                Icon(painterResource(R.drawable.ic_save_24), contentDescription = stringResource(R.string.save))
            }
        },
    )
}

internal fun clampPageIndex(index: Int, pageCount: Int): Int = if (pageCount <= 0) 0 else index.coerceIn(0, pageCount - 1)
