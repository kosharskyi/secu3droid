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

import org.secu3.android.ui.compose.DropdownShape

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

class SecurityFragment : BaseParamFragment() {
    private val state get() = mViewModel.securityStateHolder
    private var expanded by mutableStateOf(false)
    private val types = listOf("BC417", "BK3231", "BK3231S (JDY-31)", "BC352 (HC-05)", "BK3432", "BK3431S")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        expanded = false
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SecuComposeTheme { SecurityScreen(state.loaded, state.useBt, state.useImm, state.name, state.password, state.key0, state.key1, state.type, types, expanded, state.error,
                state.useReserveParams, state.checkFirmwareCrc, { expanded = it }, state::changeType, state::changeBt, state::changeImm, state::changeName, state::changePass, state::changeKey0, state::changeKey1, state::applyCredentials) } }
        }
    }

    override fun tabTitle(): Int = R.string.params_tab_security_title
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SecurityScreen(loaded: Boolean, useBt: Boolean, useImm: Boolean, name: String, pass: String, key0: String, key1: String,
    type: Int, types: List<String>, expanded: Boolean, error: String, useReserveParams: Boolean, checkFirmwareCrc: Boolean,
    expand: (Boolean) -> Unit, selectType: (Int) -> Unit,
    toggleBt: (Boolean) -> Unit, toggleImm: (Boolean) -> Unit, changeName: (String) -> Unit, changePass: (String) -> Unit,
    changeKey0: (String) -> Unit, changeKey1: (String) -> Unit, apply: () -> Unit) {
    if (!loaded) { ParameterLoadingIndicator("security-loading"); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.sec_bluetooth), style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = expand) {
                OutlinedTextField(value = types.getOrNull(type) ?: stringResource(R.string.sec_unknown_type, type), onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(R.string.sec_bt_type)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("security-type"), shape = DropdownShape)
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expand(false) }, shape = DropdownShape) {
                    types.forEachIndexed { i, label -> DropdownMenuItem(text = { Text(label) }, onClick = { selectType(i); expand(false) }) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.secur_par_use_bluetooth_title)); Switch(useBt, toggleBt, Modifier.testTag("security-bt-switch")) }
            OutlinedTextField(name, changeName, enabled = useBt, label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth().testTag("security-name"), singleLine = true)
            OutlinedTextField(pass, changePass, enabled = useBt, label = { Text(stringResource(R.string.password)) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().testTag("security-password"), singleLine = true)
            Text(stringResource(R.string.sec_credentials_write_only), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("security-credentials-hint"))
            Button(onClick = apply, enabled = useBt && name.isNotEmpty() && pass.length in 4..8, modifier = Modifier.align(androidx.compose.ui.Alignment.End).testTag("security-apply")) { Text(stringResource(R.string.apply)) }
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.sec_immobilizer), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(stringResource(R.string.secur_par_use_immobilizer_title)); Switch(useImm, toggleImm, Modifier.testTag("security-imm-switch")) }
            KeyField(key0, stringResource(R.string.sec_key1), "security-key1", useImm, changeKey0)
            KeyField(key1, stringResource(R.string.sec_key2), "security-key2", useImm, changeKey1)
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.sec_information), style = MaterialTheme.typography.titleMedium)
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
        } }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("security-error"))
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
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).testTag("$tag-title"))
            Text(
                stringResource(if (enabled) R.string.sec_enabled else R.string.sec_disabled),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp).testTag("$tag-value"),
            )
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("$tag-description"))
    }
}
