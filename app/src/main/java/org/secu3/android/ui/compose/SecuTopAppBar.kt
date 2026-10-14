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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
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
    val colorId = when (connectionState) {
        Connected -> R.color.gauge_dark_green
        InProgress -> R.color.gauge_dark_yellow
        Disconnected -> R.color.gauge_dark_red
        else -> R.color.gauge_gray
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colorResource(colorId).copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(colorResource(colorId)),
        )
        Text(
            text = stringResource(labelId),
            color = MaterialTheme.colorScheme.onSurface,
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
        title = { Text(title) },
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
