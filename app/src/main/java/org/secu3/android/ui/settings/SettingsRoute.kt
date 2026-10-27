/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.settings

import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.secu3.android.connection.ConnectionNotificationPublisher
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import org.secu3.android.ui.compose.SecuComposeTheme

@Composable
internal fun SettingsRoute(
    entry: NavBackStackEntry,
    activity: MainActivity,
    onNavigateUp: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(
        viewModelStoreOwner = entry,
        factory = activity.defaultViewModelProviderFactory,
        extras = entry.defaultViewModelCreationExtras,
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var state by remember(viewModel) { mutableStateOf(viewModel.state.value) }
    var dialog by remember(viewModel) { mutableStateOf(viewModel.dialog.value) }
    @Suppress("InlinedApi")
    val hasRuntimeNotificationPermission = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val hasConnectionNotificationAccess = {
        if (!hasRuntimeNotificationPermission() || !NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            false
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            true
        } else {
            val channel = activity.getSystemService(android.app.NotificationManager::class.java)
                .getNotificationChannel(ConnectionNotificationPublisher.CHANNEL_ID)
            channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
        }
    }
    var notificationAccessGranted by remember(activity) { mutableStateOf(hasConnectionNotificationAccess()) }
    var notificationPermissionDialog by remember { mutableStateOf(false) }
    var notificationSettingsDialog by remember { mutableStateOf(false) }
    var openNotificationChannelSettings by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        activity.userPrefs.hasRequestedConnectionNotificationPermission = true
        notificationAccessGranted = hasConnectionNotificationAccess()
        if (granted && notificationAccessGranted) activity.refreshConnectionNotification()
    }
    val requestNotificationPermission = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasRuntimeNotificationPermission()) {
            notificationAccessGranted = hasConnectionNotificationAccess()
            openNotificationChannelSettings = true
            notificationSettingsDialog = true
        } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            notificationPermissionDialog = true
        } else if (activity.userPrefs.hasRequestedConnectionNotificationPermission) {
            openNotificationChannelSettings = false
            notificationSettingsDialog = true
        } else {
            activity.userPrefs.hasRequestedConnectionNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val directoryPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        consumeFolderPickerResult(
            resultCode = result.resultCode,
            data = result.data,
            takePermission = activity.contentResolver::takePersistableUriPermission,
            save = viewModel::saveExportFolder,
        )
    }
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.state.collect { state = it } }
            launch { viewModel.dialog.collect { dialog = it } }
        }
    }
    androidx.compose.runtime.DisposableEffect(lifecycle, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val wasAllowed = notificationAccessGranted
                notificationAccessGranted = hasConnectionNotificationAccess()
                if (!wasAllowed && notificationAccessGranted) activity.refreshConnectionNotification()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    SecuComposeTheme {
        SettingsContent(
            state = state,
            dialog = dialog,
            onNavigateUp = onNavigateUp,
            onToggle = { key, value ->
                viewModel.setBooleanPreference(key, value)
                if (key == R.string.pref_wakelock_key) {
                    activity.onBackgroundWorkPreferenceChanged(value)
                    if (value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasRuntimeNotificationPermission()) {
                        requestNotificationPermission()
                    }
                }
            },
            onSelectDelimiter = viewModel::setDelimiter,
            onChooseFolder = {
                directoryPicker.launch(folderPickerIntent())
            },
            onEditRetries = { viewModel.showDialog(SettingsDialog.Retries) },
            onValidateRetries = viewModel::isValidRetries,
            onSaveRetries = viewModel::saveRetries,
            onSelectDialog = viewModel::showDialog,
            onDismissDialog = viewModel::dismissDialog,
            onShowAbout = { viewModel.showDialog(SettingsDialog.About) },
            onSelectAppearance = viewModel::setAppearanceMode,
            notificationAccessGranted = notificationAccessGranted,
            onConnectionNotifications = {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasRuntimeNotificationPermission()) {
                    notificationAccessGranted = hasConnectionNotificationAccess()
                    openNotificationChannelSettings = true
                    notificationSettingsDialog = true
                } else {
                    requestNotificationPermission()
                }
            },
        )
        if (dialog == SettingsDialog.About) {
            Dialog(onDismissRequest = viewModel::dismissDialog) {
                Box(Modifier.fillMaxWidth().widthIn(max = 560.dp).testTag("settings-about-dialog")) {
                    AboutContent(open = { url -> activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) })
                }
            }
        }
        if (notificationPermissionDialog) {
            AlertDialog(
                onDismissRequest = { notificationPermissionDialog = false },
                title = { Text(activity.getString(R.string.connection_notifications_rationale_title)) },
                text = { Text(activity.getString(R.string.connection_notifications_rationale_message)) },
                confirmButton = {
                    Button(onClick = {
                        notificationPermissionDialog = false
                        activity.userPrefs.hasRequestedConnectionNotificationPermission = true
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text(activity.getString(R.string.connection_notifications_allow)) }
                },
                dismissButton = {
                    TextButton(onClick = { notificationPermissionDialog = false }) {
                        Text(activity.getString(android.R.string.cancel))
                    }
                },
            )
        }
        if (notificationSettingsDialog) {
            AlertDialog(
                onDismissRequest = { notificationSettingsDialog = false },
                title = { Text(activity.getString(R.string.connection_notifications_settings_title)) },
                text = { Text(activity.getString(R.string.connection_notifications_settings_message)) },
                confirmButton = {
                    Button(onClick = {
                        notificationSettingsDialog = false
                        val settingsIntent = if (openNotificationChannelSettings && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", activity.packageName, null))
                        }
                        activity.startActivity(settingsIntent)
                    }) { Text(activity.getString(R.string.connection_notifications_open_settings)) }
                },
                dismissButton = {
                    TextButton(onClick = { notificationSettingsDialog = false }) {
                        Text(activity.getString(android.R.string.cancel))
                    }
                },
            )
        }
    }
}

internal fun folderPickerIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
    addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
    )
}

internal fun consumeFolderPickerResult(
    resultCode: Int,
    data: Intent?,
    takePermission: (Uri, Int) -> Unit,
    save: (String) -> Unit,
) {
    if (resultCode != Activity.RESULT_OK) return
    val uri = data?.data ?: return
    val grantedFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    if (grantedFlags != 0 && runCatching { takePermission(uri, grantedFlags) }.isFailure) return
    save(uri.toString())
}
