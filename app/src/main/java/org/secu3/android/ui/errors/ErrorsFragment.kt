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

package org.secu3.android.ui.errors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.models.CheckEngineError

@AndroidEntryPoint
class ErrorsFragment : Fragment() {

    private val mViewModel: ErrorsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val connectionState by mViewModel.connectionStatusLiveData.observeAsState()
                val savedErrorsPacket by mViewModel.checkEngineSavedLiveData.observeAsState()
                val activeErrorsPacket by mViewModel.checkEngineLiveData.observeAsState()
                val errorTitles = stringArrayResource(R.array.errors_ecu_errors_names)
                val errors = errorTitles.mapIndexed { index, title ->
                    CheckEngineError(
                        title = title,
                        isActive = activeErrorsPacket?.isError(index) ?: false,
                        isSaved = savedErrorsPacket?.isError(index) ?: false,
                    )
                }

                ErrorsScreen(
                    connectionState = connectionState,
                    errors = errors,
                    isLoading = savedErrorsPacket == null && activeErrorsPacket == null,
                    onBackClick = { findNavController().navigateUp() },
                    onClearClick = { confirmClearErrors() },
                )
            }
        }
    }

    @Composable
    private fun ErrorsScreen(
        connectionState: ConnectionState?,
        errors: List<CheckEngineError>,
        isLoading: Boolean,
        onBackClick: () -> Unit,
        onClearClick: () -> Unit,
    ) {
        val view = LocalView.current
        val displayedErrors = errors.filter { it.isActive || it.isSaved }

        SideEffect {
            requireActivity().window.statusBarColor = ErrorsBackground.toArgb()
            requireActivity().window.navigationBarColor = ErrorsBackground.toArgb()
            WindowInsetsControllerCompat(requireActivity().window, view).isAppearanceLightStatusBars = false
        }

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ErrorsBackground,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    ErrorsTopBar(
                        connectionState = connectionState,
                        onBackClick = onBackClick,
                        onClearClick = onClearClick,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ErrorsDivider),
                    )
                    if (isLoading) {
                        LoadingErrorsState(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 20.dp,
                                top = 18.dp,
                                end = 20.dp,
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(key = "summary") {
                                ErrorsSummary(errors = errors)
                            }
                            if (displayedErrors.isEmpty()) {
                                item(key = "empty") {
                                    EmptyErrorsState(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(horizontal = 4.dp),
                                    )
                                }
                            } else {
                                items(displayedErrors, key = { it.title }) { error ->
                                    ErrorCard(error = error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ErrorsTopBar(
        connectionState: ConnectionState?,
        onBackClick: () -> Unit,
        onClearClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .padding(start = 20.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back_24),
                    contentDescription = stringResource(R.string.abc_action_bar_up_description),
                    tint = ErrorsTextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.errors_title_ecu),
                color = ErrorsTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            )
            ConnectionStatusIcon(connectionState = connectionState)
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(ToolbarIconButtonSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorsRedContainer)
                    .border(1.dp, ErrorsRedStroke, RoundedCornerShape(12.dp))
                    .clickable(onClick = onClearClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outline_24),
                    contentDescription = stringResource(R.string.errors_clear_all_short),
                    tint = ErrorsRed,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    @Composable
    private fun ConnectionStatusIcon(connectionState: ConnectionState?) {
        val contentColor = when (connectionState) {
            Connected -> ErrorsGreen
            InProgress -> ErrorsAmber
            Disconnected -> ErrorsRed
            else -> ErrorsRed
        }

        Box(
            modifier = Modifier.size(ToolbarIconButtonSize),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }

    @Composable
    private fun ErrorsSummary(errors: List<CheckEngineError>) {
        val activeCount = errors.count { it.isActive }
        val savedCount = errors.count { it.isSaved }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(1.5.dp, ErrorsAmber, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = ErrorsAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.errors_summary, activeCount, savedCount),
                color = ErrorsAmber,
                fontSize = 14.sp,
            )
        }
    }

    @Composable
    private fun ErrorCard(error: CheckEngineError) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ErrorsCard)
                .border(1.dp, ErrorsCardStroke, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text(
                text = error.title,
                color = ErrorsTextPrimary,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StateBadge(
                    text = stringResource(R.string.error_state_active),
                    selected = error.isActive,
                    selectedColor = ErrorsRed,
                    selectedContainer = ErrorsRedContainer,
                    selectedStroke = ErrorsRedStroke,
                )
                StateBadge(
                    text = stringResource(R.string.error_state_saved),
                    selected = error.isSaved,
                    selectedColor = ErrorsAmber,
                    selectedContainer = ErrorsAmberContainer,
                    selectedStroke = ErrorsAmberStroke,
                )
            }
        }
    }

    @Composable
    private fun StateBadge(
        text: String,
        selected: Boolean,
        selectedColor: Color,
        selectedContainer: Color,
        selectedStroke: Color,
    ) {
        Text(
            text = text,
            color = if (selected) selectedColor else ErrorsTextDisabled,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) selectedContainer else ErrorsBadgeInactive)
                .border(
                    width = 1.dp,
                    color = if (selected) selectedStroke else ErrorsBadgeInactiveStroke,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }

    @Composable
    private fun EmptyErrorsState(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.errors_empty_state),
                color = ErrorsTextDisabled,
                fontSize = 16.sp,
            )
        }
    }

    @Composable
    private fun LoadingErrorsState(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                color = ErrorsAmber,
                trackColor = ErrorsBadgeInactive,
            )
            Text(
                text = stringResource(R.string.errors_loading_state),
                color = ErrorsTextDisabled,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }

    private fun confirmClearErrors() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_all_errors)
            .setMessage(R.string.dialog_confirm_clear_errors_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                mViewModel.clearErrors()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private companion object {
        val ErrorsBackground = Color(0xFF0E1218)
        val ErrorsCard = Color(0xFF171F27)
        val ErrorsCardStroke = Color(0xFF4A371D)
        val ErrorsDivider = Color(0xFF242B33)
        val ErrorsTextPrimary = Color(0xFFE6EDF5)
        val ErrorsTextDisabled = Color(0xFF737B85)
        val ErrorsGreen = Color(0xFF20D67A)
        val ErrorsAmber = Color(0xFFF5A61D)
        val ErrorsAmberContainer = Color(0xFF2B2417)
        val ErrorsAmberStroke = Color(0xFF765314)
        val ErrorsRed = Color(0xFFFF5C5C)
        val ErrorsRedContainer = Color(0xFF321A1F)
        val ErrorsRedStroke = Color(0xFF823139)
        val ErrorsBadgeInactive = Color(0xFF222A33)
        val ErrorsBadgeInactiveStroke = Color(0xFF3A444F)
        val ToolbarIconButtonSize = 40.dp
    }
}
