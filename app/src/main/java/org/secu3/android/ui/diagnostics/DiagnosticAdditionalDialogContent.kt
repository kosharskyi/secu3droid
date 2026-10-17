package org.secu3.android.ui.diagnostics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.secu3.android.R

@OptIn(ExperimentalLayoutApi::class)
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
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.diag_bl_de_warning),
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DiagnosticOption(stringResource(R.string.diag_enable_bl_de), enableBlDe, "enable_bl_de", { enableBlDe = it })
            if (showTachO) DiagnosticOption(stringResource(R.string.diag_enable_tach_o), enableTachO, "enable_tach_o", { enableTachO = it })
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
                Button(modifier = Modifier.testTag("diagnostic_ok"), onClick = { onOk(enableBlDe, enableTachO) }) { Text(stringResource(android.R.string.ok)) }
            }
        }
    }
}

@Composable
private fun DiagnosticOption(label: String, checked: Boolean, testTag: String, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).then(modifier).testTag(testTag).clickable { onCheckedChange(!checked) }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag("${testTag}_checkbox"))
    }
}
