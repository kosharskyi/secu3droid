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
package org.secu3.android.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme
import org.secu3.android.ui.compose.SecuTopAppBar

data class SettingsUiState(
    val writeLog: Boolean,
    val writeCsvHeader: Boolean,
    val csvDelimiter: String,
    val writeBinaryLog: Boolean,
    val exportFolder: String,
    val connectionRetries: String,
    val keepScreenOn: Boolean,
    val wakeLock: Boolean,
)

/** Returns a positive Int value that remains safe for UserPrefs.connectionRetries.toInt(). */
internal fun parsePositiveRetries(value: String): Int? = value.toIntOrNull()?.takeIf { it > 0 }

class SettingsFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var sharedPref: SharedPreferences
    private var settingsState by mutableStateOf<SettingsUiState?>(null)
    private var dialog by mutableStateOf<SettingsDialog?>(null)

    private val directoryRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        val flags = result.data?.flags ?: 0
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (takeFlags != 0) requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
        sharedPref.edit { putString(getString(R.string.pref_log_export_directory_key), uri.toString()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        settingsState = readSettings()
    }

    override fun onStart() {
        super.onStart()
        sharedPref.registerOnSharedPreferenceChangeListener(this)
        settingsState = readSettings()
    }

    override fun onStop() {
        sharedPref.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        settingsState = readSettings()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    val state = settingsState
                    if (state != null) {
                        SettingsContent(
                            state = state,
                            dialog = dialog,
                            onNavigateUp = { requireActivity().finish() },
                            onToggle = ::setBooleanPreference,
                            onSelectDelimiter = ::setDelimiter,
                            onChooseFolder = ::openLogExportDirectoryPicker,
                            onEditRetries = { dialog = SettingsDialog.Retries },
                            onSaveRetries = ::saveRetries,
                            onSelectDialog = { dialog = it },
                            onDismissDialog = { dialog = null },
                            onShowAbout = { AboutDialog().show(childFragmentManager, "About dialog") },
                        )
                    }
                }
            }
        }

    private fun readSettings(): SettingsUiState {
        val delimiterKey = getString(R.string.pref_log_csv_delimeter_key)
        val retriesKey = getString(R.string.pref_connection_retries_key)
        val exportFolder = sharedPref.getString(getString(R.string.pref_log_export_directory_key), null)
        return SettingsUiState(
            writeLog = sharedPref.getBoolean(getString(R.string.pref_write_log_key), false),
            writeCsvHeader = sharedPref.getBoolean(getString(R.string.pref_log_csv_write_title_key), false),
            csvDelimiter = sharedPref.getString(delimiterKey, ";") ?: ";",
            writeBinaryLog = sharedPref.getBoolean(getString(R.string.pref_write_binary_log_key), false),
            exportFolder = exportFolder?.let(::folderSummary) ?: getString(R.string.pref_log_export_directory_default),
            connectionRetries = sharedPref.getString(retriesKey, getString(R.string.defaultConnectionRetries))
                ?: getString(R.string.defaultConnectionRetries),
            keepScreenOn = sharedPref.getBoolean(getString(R.string.pref_keep_screen_key), false),
            wakeLock = sharedPref.getBoolean(getString(R.string.pref_wakelock_key), false),
        )
    }

    private fun folderSummary(uri: String): String = runCatching {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(uri.toUri())
        treeDocumentId.substringAfter(':').ifBlank { treeDocumentId }
    }.getOrDefault(getString(R.string.pref_log_export_directory_default))

    private fun setBooleanPreference(keyResource: Int, value: Boolean) {
        sharedPref.edit { putBoolean(getString(keyResource), value) }
    }

    private fun setDelimiter(value: String) {
        sharedPref.edit { putString(getString(R.string.pref_log_csv_delimeter_key), value) }
        dialog = null
    }

    private fun saveRetries(value: String) {
        if (parsePositiveRetries(value) == null) return
        sharedPref.edit { putString(getString(R.string.pref_connection_retries_key), value) }
        dialog = null
    }

    private fun openLogExportDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
            )
        }
        directoryRequest.launch(intent)
    }

    @Composable
    internal fun SettingsContent(
        state: SettingsUiState,
        dialog: SettingsDialog?,
        onNavigateUp: () -> Unit,
        onToggle: (Int, Boolean) -> Unit,
        onSelectDelimiter: (String) -> Unit,
        onChooseFolder: () -> Unit,
        onEditRetries: () -> Unit,
        onSaveRetries: (String) -> Unit,
        onSelectDialog: (SettingsDialog?) -> Unit,
        onDismissDialog: () -> Unit,
        onShowAbout: () -> Unit,
    ) {
        val delimiterValues = stringArrayResource(R.array.csv_delimeters_values)
        val delimiterLabels = stringArrayResource(R.array.csv_delimeters)
        val delimiterIndex = delimiterValues.indexOf(state.csvDelimiter).takeIf { it >= 0 } ?: 1
        Scaffold(topBar = {
            SecuTopAppBar(
                title = stringResource(R.string.title_activity_settings),
                onNavigateUp = onNavigateUp,
                showStatus = false,
            )
        }) { contentPadding ->
            Column(
                Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            ) {
                BooleanSetting(R.string.pref_write_log_title, R.string.pref_write_log_key, R.string.pref_write_log_summary, state.writeLog) {
                    onToggle(R.string.pref_write_log_key, it)
                }
                BooleanSetting(R.string.pref_log_csv_write_title, R.string.pref_log_csv_write_title_key, null, state.writeCsvHeader) {
                    onToggle(R.string.pref_log_csv_write_title_key, it)
                }
                SettingRow(stringResource(R.string.pref_log_csv_delimeter_title), delimiterLabels[delimiterIndex]) {
                    onSelectDialog(SettingsDialog.Delimiter)
                }
                BooleanSetting(R.string.pref_write_binary_log_title, R.string.pref_write_binary_log_key, null, state.writeBinaryLog) {
                    onToggle(R.string.pref_write_binary_log_key, it)
                }
                SettingRow(stringResource(R.string.pref_log_export_directory_title), state.exportFolder, onChooseFolder)
                SettingRow(
                    stringResource(R.string.pref_connection_retries_title),
                    stringResource(R.string.pref_connection_retries_summary, state.connectionRetries),
                    onEditRetries,
                )
                BooleanSetting(R.string.pref_keep_screen_title, R.string.pref_keep_screen_key, R.string.pref_keep_screen_summary, state.keepScreenOn) {
                    onToggle(R.string.pref_keep_screen_key, it)
                }
                BooleanSetting(R.string.pref_wakelock_title, R.string.pref_wakelock_key, R.string.pref_wakelock_summary, state.wakeLock) {
                    onToggle(R.string.pref_wakelock_key, it)
                }
                SettingRow(stringResource(R.string.pref_about_title), stringResource(R.string.pref_about_summary), onShowAbout)
            }
        }

        when (dialog) {
            SettingsDialog.Delimiter -> DelimiterDialog(
                selected = state.csvDelimiter,
                labels = delimiterLabels.toList(),
                values = delimiterValues.toList(),
                onSelect = onSelectDelimiter,
                onDismiss = onDismissDialog,
            )
            SettingsDialog.Retries -> RetryDialog(
                initialValue = state.connectionRetries,
                onConfirm = onSaveRetries,
                onDismiss = onDismissDialog,
            )
            null -> Unit
        }
    }

    @Composable
    private fun BooleanSetting(titleId: Int, keyId: Int, summaryId: Int?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
            Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(titleId))
                if (summaryId != null) Text(stringResource(summaryId), style = MaterialTheme.typography.bodySmall)
            }
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag("settings-toggle-$keyId"))
        }
    }

    @Composable
    private fun SettingRow(title: String, summary: String, onClick: () -> Unit) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title)
            if (summary.isNotEmpty()) Text(summary, style = MaterialTheme.typography.bodySmall)
        }
    }

    @Composable
    private fun DelimiterDialog(
        selected: String,
        labels: List<String>,
        values: List<String>,
        onSelect: (String) -> Unit,
        onDismiss: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.pref_log_csv_delimeter_title)) },
            text = {
                Column {
                    labels.forEachIndexed { index, label ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onSelect(values[index]) }.padding(8.dp),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            RadioButton(selected = selected == values[index], onClick = { onSelect(values[index]) })
                            Text(label, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
        )
    }

    @Composable
    private fun RetryDialog(initialValue: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
        var text by androidx.compose.runtime.remember(initialValue) { mutableStateOf(initialValue) }
        val parsedValue = parsePositiveRetries(text)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.pref_connection_retries_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter(Char::isDigit) },
                    singleLine = true,
                    isError = parsedValue == null,
                )
            },
            confirmButton = {
                TextButton(onClick = { parsedValue?.let { onConfirm(it.toString()) } }, enabled = parsedValue != null) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        )
    }

    internal enum class SettingsDialog { Delimiter, Retries }
}
