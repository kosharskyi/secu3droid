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
package org.secu3.android.ui.parameters.pages

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.secu3.android.R
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.ui.parameters.compose.ParameterPage
import org.secu3.android.ui.parameters.compose.ParameterSection
import org.secu3.android.ui.parameters.compose.ParameterSelectionRow
import org.secu3.android.ui.parameters.compose.ParameterSwitchRow
import org.secu3.android.ui.parameters.dialogs.ParameterChoice
import org.secu3.android.ui.parameters.dialogs.ParameterChoiceDialog

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SecurityScreen(loaded: Boolean, useBt: Boolean, useImm: Boolean, name: String, pass: String, key0: String, key1: String,
    type: Int, types: List<String>, expanded: Boolean, error: String, useReserveParams: Boolean, checkFirmwareCrc: Boolean,
    expand: (Boolean) -> Unit, selectType: (Int) -> Unit,
    toggleBt: (Boolean) -> Unit, toggleImm: (Boolean) -> Unit, changeName: (String) -> Unit, changePass: (String) -> Unit,
    changeKey0: (String) -> Unit, changeKey1: (String) -> Unit, apply: () -> Unit) {
    val pageScrollState = rememberScrollState()
    if (!loaded) { ParameterLoadingIndicator("security-loading"); return }
    ParameterPage(scrollState = pageScrollState) {
        ParameterSection(stringResource(R.string.sec_bluetooth)) {
            Box {
                ParameterSelectionRow(
                    title = stringResource(R.string.sec_bt_type),
                    value = types.getOrNull(type) ?: stringResource(R.string.sec_unknown_type, type),
                    testTag = "security-type",
                    onClick = { expand(!expanded) },
                )
                if (expanded) ParameterChoiceDialog(
                    title = stringResource(R.string.sec_bt_type),
                    choices = types.mapIndexed { i, label -> ParameterChoice(i, label, "security_type_option_$i") },
                    onDismiss = { expand(false) },
                    onSelect = selectType,
                )
            }
            ParameterSwitchRow(stringResource(R.string.secur_par_use_bluetooth_title), useBt, "security-bt", toggleBt)
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, changeName, enabled = useBt, label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth().testTag("security-name"), singleLine = true)
                OutlinedTextField(pass, changePass, enabled = useBt, label = { Text(stringResource(R.string.password)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().testTag("security-password"), singleLine = true)
                Text(stringResource(R.string.sec_credentials_write_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("security-credentials-hint"))
                Button(onClick = apply, enabled = useBt && name.isNotEmpty() && pass.length in 4..8, modifier = Modifier.align(Alignment.End).padding(bottom = 12.dp).testTag("security-apply")) { Text(stringResource(R.string.apply)) }
            }
        }
        ParameterSection(stringResource(R.string.sec_immobilizer)) {
            ParameterSwitchRow(stringResource(R.string.secur_par_use_immobilizer_title), useImm, "security-imm", toggleImm)
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KeyField(key0, stringResource(R.string.sec_key1), "security-key1", useImm, changeKey0)
                KeyField(key1, stringResource(R.string.sec_key2), "security-key2", useImm, changeKey1)
            }
        }
        ParameterSection(stringResource(R.string.sec_information)) {
            SecurityFlagStatus(
                title = stringResource(R.string.secur_par_always_load_params_from_flash_title),
                enabled = useReserveParams,
                description = stringResource(R.string.sec_reserve_params_description),
                tag = "security-reserve-readonly",
            )
            SecurityFlagStatus(
                title = stringResource(R.string.secur_par_check_firmware_integrity_title),
                enabled = checkFirmwareCrc,
                description = stringResource(R.string.sec_firmware_integrity_description),
                tag = "security-crc-readonly",
            )
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 14.dp).testTag("security-error"))
    }
}

@Composable
private fun KeyField(value: String, label: String, tag: String, enabled: Boolean, onChange: (String) -> Unit) {
    val valid = parseIButtonKey(value) != null
    val invalidCompleteValue = value.length == 12 && !valid
    OutlinedTextField(value, onChange, enabled = enabled, label = { Text(label) }, singleLine = true,
        isError = invalidCompleteValue,
        supportingText = { Text(stringResource(if (invalidCompleteValue) R.string.sec_ibutton_key_invalid else R.string.sec_ibutton_key_hint)) },
        modifier = Modifier.fillMaxWidth().testTag(tag))
}

@Composable
private fun SecurityFlagStatus(title: String, enabled: Boolean, description: String, tag: String) {
    Column(Modifier.fillMaxWidth().testTag(tag)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(horizontal = 14.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).testTag("$tag-title"))
            Text(
                stringResource(if (enabled) R.string.sec_enabled else R.string.sec_disabled),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("$tag-value"),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).testTag("$tag-description"))
    }
}
