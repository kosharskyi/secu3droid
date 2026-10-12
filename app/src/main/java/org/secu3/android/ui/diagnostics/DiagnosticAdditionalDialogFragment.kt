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

package org.secu3.android.ui.diagnostics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.navigation.navGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.secu3.android.R
import org.secu3.android.ui.compose.SecuComposeTheme

@AndroidEntryPoint
class DiagnosticAdditionalDialogFragment : DialogFragment() {

    private val mViewModel: DiagnosticsViewModel by navGraphViewModels(
        R.id.diagnosticsFragment,
        factoryProducer = { defaultViewModelProviderFactory },
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.diagnostic_additional_dialog_content
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SecuComposeTheme {
                    DiagnosticAdditionalDialogContent(
                        initialEnableBlDe = mViewModel.enableBlDe.value ?: false,
                        initialEnableTachO = mViewModel.enableTachO.value ?: false,
                        showTachO = mViewModel.firmwareInfo?.isSecu3T != true,
                        onOk = { enableBlDe, enableTachO ->
                            mViewModel.enableBlDe(enableBlDe)
                            mViewModel.enableTachO(enableTachO)
                            dismiss()
                        },
                        onCancel = { dismiss() },
                    )
                }
            }
        }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

@Composable
internal fun DiagnosticAdditionalDialogContent(
    initialEnableBlDe: Boolean,
    initialEnableTachO: Boolean,
    showTachO: Boolean,
    onOk: (enableBlDe: Boolean, enableTachO: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var enableBlDe by rememberSaveable { mutableStateOf(initialEnableBlDe) }
    var enableTachO by rememberSaveable { mutableStateOf(initialEnableTachO) }

    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.diag_bl_de_warning),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )

            DiagnosticOption(
                label = stringResource(R.string.diag_enable_bl_de),
                checked = enableBlDe,
                testTag = "enable_bl_de",
                onCheckedChange = { enableBlDe = it },
                modifier = Modifier.padding(top = 24.dp),
            )
            if (showTachO) {
                DiagnosticOption(
                    label = stringResource(R.string.diag_enable_tach_o),
                    checked = enableTachO,
                    testTag = "enable_tach_o",
                    onCheckedChange = { enableTachO = it },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
                TextButton(
                    modifier = Modifier.testTag("diagnostic_ok"),
                    onClick = { onOk(enableBlDe, enableTachO) },
                ) { Text(stringResource(android.R.string.ok)) }
            }
        }
    }
}

@Composable
private fun DiagnosticOption(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().testTag(testTag).clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Checkbox(
            modifier = Modifier.testTag("${testTag}_checkbox"),
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
