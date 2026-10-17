package org.secu3.android.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.connection.Connected
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress

/** A small status pill whose visible label also communicates state to screen readers. */
@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState?,
    modifier: Modifier = Modifier,
) {
    @StringRes val labelId = when (connectionState) {
        Connected -> R.string.dashboard_status_online
        InProgress -> R.string.dashboard_status_connecting
        Disconnected -> R.string.dashboard_status_offline
        else -> R.string.dashboard_status_no_data
    }
    val palette = LocalSecuPalette.current
    val indicatorColor = when (connectionState) {
        Connected -> palette.success
        InProgress -> palette.warning
        Disconnected -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    val backgroundColor = when (connectionState) {
        Connected -> palette.successContainer
        InProgress -> palette.warningContainer
        Disconnected -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (connectionState) {
        Connected -> palette.onSuccessContainer
        InProgress -> palette.onWarningContainer
        Disconnected -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor),
        )
        Text(
            text = stringResource(labelId),
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
        )
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
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = {
            if (showStatus) {
                ConnectionStatusIndicator(connectionState, Modifier.padding(end = 8.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
