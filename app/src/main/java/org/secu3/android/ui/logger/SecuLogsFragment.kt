/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
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

package org.secu3.android.ui.logger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.secu3.android.R
import org.secu3.android.utils.sizeStr
import java.io.File

@AndroidEntryPoint
class SecuLogsFragment : Fragment() {

    private val mViewModel: SecuLogsViewModel by viewModels()
    private var pendingDownloadFile: File? = null
    private var snackbarHostState: SnackbarHostState? = null

    private val storagePermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        val file = pendingDownloadFile
        pendingDownloadFile = null

        if (isGranted && file != null) {
            saveLog(file)
        } else {
            showSnackbar(getString(R.string.log_export_permission_denied))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val snackbarHost = remember { SnackbarHostState() }
                var files by remember { mutableStateOf(mViewModel.getListOfLogFiles) }
                snackbarHostState = snackbarHost

                SecuLogsScreen(
                    files = files,
                    snackbarHostState = snackbarHost,
                    onBackClick = { findNavController().navigateUp() },
                    onShareClick = { share(it) },
                    onDownloadClick = { download(it) },
                    onDeleteClick = {
                        it.delete()
                        files = mViewModel.getListOfLogFiles
                    },
                )
            }
        }
    }

    private fun download(file: File) {
        if (mViewModel.requiresDefaultDownloadsPermission() && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingDownloadFile = file
            storagePermissionRequest.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        saveLog(file)
    }

    private fun saveLog(file: File) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = mViewModel.saveLogFile(file)
            val message = if (result.isSuccess) {
                getString(R.string.log_export_success, result.destination)
            } else {
                getString(R.string.log_export_failed)
            }
            showSnackbar(message)
        }
    }

    private fun showSnackbar(message: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            snackbarHostState?.showSnackbar(message)
        }
    }

    private fun share(file: File) {
        val shareIntent: Intent = mViewModel.getShareIntent(file)
        startActivity(Intent.createChooser(shareIntent, null))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecuLogsScreen(
    files: List<File>,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onShareClick: (File) -> Unit,
    onDownloadClick: (File) -> Unit,
    onDeleteClick: (File) -> Unit,
) {
    var filePendingDelete by remember { mutableStateOf<File?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LogsPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = LogsBackground,
    ) { innerPadding ->
        if (files.isEmpty()) {
            EmptyLogsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                items(files, key = { it.absolutePath }) { file ->
                    LogFileCard(
                        file = file,
                        onShareClick = { onShareClick(file) },
                        onDownloadClick = { onDownloadClick(file) },
                        onDeleteClick = { filePendingDelete = file },
                    )
                }
            }
        }
    }

    filePendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { filePendingDelete = null },
            title = { Text(text = stringResource(R.string.dialog_confirm_delete_log_file_title)) },
            text = { Text(text = stringResource(R.string.dialog_confirm_delete_log_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        filePendingDelete = null
                        onDeleteClick(file)
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { filePendingDelete = null }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyLogsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_logs_list),
            color = LogsTextSecondary,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun LogFileCard(
    file: File,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LogsCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = file.name,
                color = LogsTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = file.sizeStr(),
                color = LogsTextSecondary,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_outline_24),
                        contentDescription = stringResource(R.string.delete_log_file),
                        tint = LogsDanger,
                    )
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share_24),
                        contentDescription = stringResource(R.string.share_log_file),
                        tint = LogsTextPrimary,
                    )
                }
                Button(
                    onClick = onDownloadClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LogsAccent),
                ) {
                    Text(text = stringResource(R.string.save))
                }
            }
        }
    }
}

private val LogsBackground = Color(0xFFF4F7FA)
private val LogsPrimary = Color(0xFF263A4D)
private val LogsCard = Color.White
private val LogsAccent = Color(0xFF1B7F6B)
private val LogsDanger = Color(0xFFB4443F)
private val LogsTextPrimary = Color(0xFF17212B)
private val LogsTextSecondary = Color(0xFF6E7E8D)
