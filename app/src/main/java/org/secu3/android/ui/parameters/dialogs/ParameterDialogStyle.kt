package org.secu3.android.ui.parameters.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import org.secu3.android.R

internal data class ParameterChoice(val id: Int, val label: String, val testTag: String)

private val ParameterDialogShape = RoundedCornerShape(24.dp)
private val ParameterOptionShape = RoundedCornerShape(12.dp)

@Composable
internal fun ParameterAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)) {
                    title()
                }
            }
        },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, ParameterDialogShape),
        shape = ParameterDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    )
}

@Composable
internal fun ParameterChoiceList(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
internal fun ParameterChoiceOption(
    text: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(ParameterOptionShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(testTag),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun ParameterChoiceDialog(
    title: String,
    choices: List<ParameterChoice>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    ParameterAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            ParameterChoiceList {
                choices.forEach { choice ->
                    ParameterChoiceOption(choice.label, choice.testTag) {
                        onSelect(choice.id)
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = { ParameterCancelButton(onDismiss) },
    )
}

@Composable
internal fun ParameterCancelButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(android.R.string.cancel))
    }
}

@Composable
internal fun ParameterConfirmButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text)
    }
}
