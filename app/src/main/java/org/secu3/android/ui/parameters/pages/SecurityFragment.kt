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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import org.secu3.android.ui.parameters.compose.ParameterLoadingIndicator
import org.secu3.android.R
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import org.secu3.android.ui.compose.SecuComposeTheme

class SecurityFragment : BaseParamFragment() {
    private var packet: SecurityParamPacket? = null
    private var loaded by mutableStateOf(false)
    private var name by mutableStateOf("")
    private var password by mutableStateOf("")
    private var key0 by mutableStateOf("")
    private var key1 by mutableStateOf("")
    private var error by mutableStateOf("")
    private var useBt by mutableStateOf(false)
    private var useImm by mutableStateOf(false)
    private var type by mutableIntStateOf(0)
    private var useReserveParams by mutableStateOf(false)
    private var checkFirmwareCrc by mutableStateOf(false)
    private var expanded by mutableStateOf(false)
    private val types = listOf("BC417", "BK3231", "BK3231S (JDY-31)", "BC352 (HC-05)", "BK3432", "BK3431S")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        packet = null
        loaded = false
        name = ""; password = ""; key0 = ""; key1 = ""; error = ""
        useBt = false; useImm = false; type = 0
        useReserveParams = false; checkFirmwareCrc = false; expanded = false
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SecuComposeTheme { SecurityScreen(loaded, useBt, useImm, name, password, key0, key1, type, types, expanded, error,
                useReserveParams, checkFirmwareCrc, { expanded = it }, ::changeType, ::changeBt, ::changeImm, ::changeName, ::changePass, ::changeKey0, ::changeKey1, ::applyCredentials) } }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.withResumed {
                mViewModel.securityLiveData.observe(viewLifecycleOwner) { p ->
                    mViewModel.isSendAllowed = false
                    packet = p
                    useBt = p.useBt; useImm = p.useImmobilizer; type = p.btType
                    useReserveParams = p.useReserveParams; checkFirmwareCrc = p.checkFwCrc
                    key0 = formatKey(p.iButton0); key1 = formatKey(p.iButton1)
                    // Credentials are intentionally write-only in the ECU protocol.
                    name = ""; password = ""
                    loaded = true
                    mViewModel.isSendAllowed = true
                }
            }
        }
    }

    private fun commit(change: SecurityParamPacket.() -> Unit) {
        val p = packet ?: return
        try {
            change(p)
            error = ""
            mViewModel.sendPacket(p.packedSnapshot())
        } catch (e: IllegalArgumentException) { error = e.message.orEmpty() }
    }
    private fun changeType(index: Int) { type = index; commit { btType = index } }
    private fun changeBt(value: Boolean) { useBt = value; commit { useBt = value; btFlags = btFlags.setBit(1) } }
    private fun changeImm(value: Boolean) { useImm = value; commit { useImmobilizer = value } }
    private fun changeName(value: String) { if (value.length <= 16) name = value }
    private fun changePass(value: String) { if (value.length <= 8 && value.all { it in '0'..'9' }) password = value }
    private fun changeKey0(value: String) { if (value.length <= 12) { key0 = value.uppercase(); updateKey(key0, true) } }
    private fun changeKey1(value: String) { if (value.length <= 12) { key1 = value.uppercase(); updateKey(key1, false) } }
    private fun updateKey(value: String, first: Boolean) {
        val bytes = parseIButtonKey(value) ?: return
        commit { if (first) iButton0 = bytes else iButton1 = bytes }
    }
    private fun applyCredentials() {
        val p = packet ?: return
        try {
            val snapshot = p.credentialsSnapshot(name, password)
            error = ""
            mViewModel.sendPacket(snapshot)
            name = ""
            password = ""
        } catch (e: IllegalArgumentException) {
            error = getString(R.string.sec_invalid_credentials)
        }
    }

    override fun tabTitle(): Int = R.string.params_tab_security_title

    companion object {
        private fun formatKey(bytes: List<Int>) = if (bytes.size == 6) bytes.asReversed().joinToString("") { "%02X".format(it) } else ""
    }
}

internal fun parseIButtonKey(value: String): List<Int>? {
    if (value.length != 12 || !value.all { it.digitToIntOrNull(16) != null }) return null
    return value.chunked(2).reversed().map { it.toInt(16) }
}

private fun Int.setBit(bit: Int) = this or (1 shl bit)

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
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("security-type"))
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expand(false) }) {
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
