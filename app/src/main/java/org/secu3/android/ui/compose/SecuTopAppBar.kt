package org.secu3.android.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.InProgress
import org.secu3.android.connection.Disconnected

/** Compact, fixed-width toolbar status with the full state label exposed to accessibility services. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState?,
    modifier: Modifier = Modifier,
    contentDescriptionOverride: String? = null,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
) {
    @StringRes val labelId = when (connectionState) {
        Connected -> R.string.home_connection_connected
        InProgress -> R.string.home_connection_connecting
        Disconnected -> R.string.home_connection_disconnected
        else -> R.string.connection_status_unknown
    }
    val palette = LocalSecuPalette.current
    val indicatorColor = when (connectionState) {
        Connected -> palette.success
        InProgress -> palette.warning
        Disconnected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    val label = contentDescriptionOverride ?: stringResource(labelId)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .then(
                    if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick)
                    else Modifier,
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_car),
                contentDescription = null,
                tint = indicatorColor,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/** Shared toolbar for Compose destinations; navigation remains the caller's responsibility. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuTopAppBar(
    title: String,
    connectionState: ConnectionState? = null,
    onNavigateUp: (() -> Unit)? = null,
    showStatus: Boolean = connectionState != null,
    actions: @Composable () -> Unit = {},
    navigationIconModifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp, modifier = navigationIconModifier) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = {
            actions()
            if (showStatus) {
                ConnectionStatusIndicator(
                    connectionState,
                    Modifier.padding(end = 4.dp),
                    testTag = "connection-status-indicator",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
