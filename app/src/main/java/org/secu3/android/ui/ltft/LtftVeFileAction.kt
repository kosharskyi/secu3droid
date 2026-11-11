package org.secu3.android.ui.ltft

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.domain.ltft.LtftVeImage

@Composable
internal fun rememberLtftVeFileAction(
    workflow: LtftVeFileWorkflow,
    resolver: ContentResolver,
): ((List<List<Float>>) -> Unit)? {
    val state by workflow.state.collectAsStateWithLifecycle()
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        workflow.onOpenResult(resolver, uri)
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        workflow.onSaveResult(resolver, uri)
    }

    (state as? LtftVeFileWorkflow.State.Choosing)?.let { choice ->
        LtftVeFileDialog(
            choice = choice,
            onSelectSet = workflow::selectSet,
            onSelectSource = workflow::selectSource,
            onCancel = workflow::cancelSelection,
            onSave = {
                workflow.beginSave()?.let { name ->
                    try {
                        save.launch(name)
                    } catch (_: ActivityNotFoundException) {
                        workflow.onSavePickerUnavailable()
                    }
                }
            },
        )
    }

    return if (state == LtftVeFileWorkflow.State.Idle) {
        { tables ->
            if (workflow.beginOpen(tables)) {
                try {
                    open.launch(arrayOf("*/*"))
                } catch (_: ActivityNotFoundException) {
                    workflow.onOpenPickerUnavailable()
                }
            }
        }
    } else {
        null
    }
}

@Composable
private fun LtftVeFileDialog(
    choice: LtftVeFileWorkflow.State.Choosing,
    onSelectSet: (Int) -> Unit,
    onSelectSource: (LtftVeImage.Source) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ltft_ve_apply)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.ltft_ve_matching_setup))
                Text(
                    stringResource(R.string.ltft_ve_source),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Column(Modifier.selectableGroup()) {
                    LtftVeImage.Source.entries.forEach { source ->
                        val title = when (source) {
                            LtftVeImage.Source.Ltft1 -> stringResource(R.string.ltft_maps_table, 1)
                            LtftVeImage.Source.Ltft2 -> stringResource(R.string.ltft_maps_table, 2)
                            LtftVeImage.Source.Average -> stringResource(R.string.ltft_ve_average)
                        }
                        LtftVeChoice(title, choice.source == source) { onSelectSource(source) }
                    }
                }
                Text(
                    stringResource(if (choice.image.isFlash) R.string.ltft_ve_flash_set else R.string.ltft_ve_eeprom_set),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (choice.image.isFlash) {
                    Column(Modifier.selectableGroup()) {
                        choice.image.setNames.forEachIndexed { index, name ->
                            val title = if (name.isBlank()) stringResource(R.string.ltft_ve_set_number, index + 1)
                            else stringResource(R.string.ltft_ve_named_set, index + 1, name)
                            LtftVeChoice(title, choice.setIndex == index) { onSelectSet(index) }
                        }
                    }
                } else {
                    choice.image.setNames.single().takeIf { it.isNotBlank() }?.let { Text(it) }
                }
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) } },
        confirmButton = { Button(onClick = onSave) { Text(stringResource(R.string.ltft_ve_save_copy)) } },
    )
}

@Composable
private fun LtftVeChoice(title: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(title, Modifier.padding(start = 12.dp))
    }
}

internal class LtftVeFileWorkflow(
    private val codec: LtftVeImage,
    private val scope: CoroutineScope,
    private val showMessage: (Int) -> Unit,
) {
    sealed interface State {
        data object Idle : State
        data class Opening(val tables: List<List<Float>>) : State
        data class Choosing(
            val tables: List<List<Float>>,
            val input: Uri,
            val image: LtftVeImage.Image,
            val setIndex: Int = 0,
            val source: LtftVeImage.Source = LtftVeImage.Source.Ltft1,
        ) : State
        data class Saving(val choice: Choosing) : State
    }

    private val mutableState = MutableStateFlow<State>(State.Idle)
    val state = mutableState.asStateFlow()

    fun beginOpen(tables: List<List<Float>>): Boolean {
        if (mutableState.value != State.Idle) return false
        mutableState.value = State.Opening(tables.map { it.toList() })
        return true
    }

    fun onOpenResult(resolver: ContentResolver, uri: Uri?) {
        val request = mutableState.value as? State.Opening ?: return
        if (uri == null) {
            mutableState.value = State.Idle
            return
        }
        scope.launch {
            when (val result = codec.read(resolver, uri)) {
                is LtftVeImage.ReadResult.Opened -> {
                    mutableState.value = State.Choosing(request.tables, uri, result.image)
                }
                LtftVeImage.ReadResult.Invalid -> finishOpenFailure(R.string.ltft_ve_invalid_file)
                LtftVeImage.ReadResult.Failed -> finishOpenFailure(R.string.ltft_ve_read_error)
            }
        }
    }

    fun selectSet(setIndex: Int) {
        val choice = mutableState.value as? State.Choosing ?: return
        mutableState.value = choice.copy(setIndex = setIndex)
    }

    fun selectSource(source: LtftVeImage.Source) {
        val choice = mutableState.value as? State.Choosing ?: return
        mutableState.value = choice.copy(source = source)
    }

    fun cancelSelection() {
        if (mutableState.value is State.Choosing) mutableState.value = State.Idle
    }

    fun beginSave(): String? {
        val choice = mutableState.value as? State.Choosing ?: return null
        mutableState.value = State.Saving(choice)
        return if (choice.image.isFlash) "LTFT-VE-FLASH.bin" else "LTFT-VE-EEPROM.bin"
    }

    fun onSaveResult(resolver: ContentResolver, uri: Uri?) {
        val choice = (mutableState.value as? State.Saving)?.choice ?: return
        if (uri == null || uri == choice.input) {
            mutableState.value = choice
            if (uri != null) showMessage(R.string.ltft_ve_write_error)
            return
        }
        scope.launch {
            val saved = codec.write(resolver, uri, choice.image, choice.setIndex, choice.source, choice.tables)
            mutableState.value = if (saved) State.Idle else choice
            showMessage(if (saved) R.string.ltft_ve_saved else R.string.ltft_ve_write_error)
        }
    }

    fun onOpenPickerUnavailable() = finishOpenFailure(R.string.ltft_ve_read_error)

    fun onSavePickerUnavailable() {
        val choice = (mutableState.value as? State.Saving)?.choice ?: return
        mutableState.value = choice
        showMessage(R.string.ltft_ve_write_error)
    }

    private fun finishOpenFailure(@StringRes message: Int) {
        mutableState.value = State.Idle
        showMessage(message)
    }
}
