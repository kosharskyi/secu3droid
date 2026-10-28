package org.secu3.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import java.io.IOException
import org.secu3.android.BuildConfig
import org.secu3.android.connection.ConnectionState
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.UserPrefs
import org.threeten.bp.LocalDate
import org.secu3.android.R
import org.secu3.android.ui.messages.UiMessageDuration
import org.secu3.android.ui.messages.UiMessageQueue
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val manager: Secu3ConnectionManager,
    private val repository: HomeRepository,
    private val appPrefs: AppPrefs,
    val prefs: UserPrefs,
    private val uiMessageQueue: UiMessageQueue,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private val _returnToStart = MutableStateFlow(false)
    val returnToStart = _returnToStart.asStateFlow()
    private var release: GitHubRelease? = null
    val firmwareInfo = manager.firmwareInfo
    val isConnectionRunning get() = manager.isConnectionRunning
    val isUsbSessionLocked get() = manager.isUsbSessionLocked

    init {
        viewModelScope.launch {
            manager.connectionStateFlow.collect { status ->
                val mapped = status.takeIf { it == Connected || it == InProgress } ?: Disconnected
                _uiState.value = _uiState.value.copy(
                    connectionStatus = mapped,
                    usbSessionLocked = manager.isUsbSessionLocked,
                    firmwareAvailable = status == Connected && manager.firmwareInfo.value != null,
                )
            }
        }
        viewModelScope.launch {
            try {
                val now = LocalDate.now()
                if (BuildConfig.DEBUG || appPrefs.lastAppVersionCheck.first().isBefore(now)) {
                    release = repository.getNewRelease()
                    val item = release
                    if (item != null) _uiState.value = _uiState.value.copy(
                        dialog = HomeDialog.RELEASE, releaseName = item.displayName,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: IOException) {
                uiMessageQueue.publish(R.string.preferences_storage_error, UiMessageDuration.LONG)
            }
        }
        viewModelScope.launch {
            manager.firmwareInfo.collect { firmware ->
                _uiState.value = _uiState.value.copy(
                    firmwareAvailable = firmware != null && _uiState.value.connectionStatus == Connected && manager.isConnected,
                )
            }
        }
    }
    fun requestExit() { if (!isUsbSessionLocked) _uiState.value = _uiState.value.copy(dialog = HomeDialog.EXIT) }
    fun dismissDialog() { _uiState.value = _uiState.value.copy(dialog = null) }
    fun confirmExit() {
        if (isUsbSessionLocked || _returnToStart.value) return
        dismissDialog()
        if (manager.stopConnection()) _returnToStart.value = true
    }
    fun consumeReturnToStart() {
        _returnToStart.value = false
    }
    fun requestDiagnostics() { _uiState.value = _uiState.value.copy(dialog = HomeDialog.DIAGNOSTICS) }
    fun requestReleasePermissionRationale() { _uiState.value = _uiState.value.copy(dialog = HomeDialog.RELEASE_PERMISSION_RATIONALE) }
    fun pendingRelease() = release
    private val releasePermissionFlow = PendingReleasePermissionFlow()
    fun beginReleasePermission(item: GitHubRelease) { releasePermissionFlow.begin(item) }
    fun completeReleasePermission(granted: Boolean) {
        releasePermissionFlow.complete(granted)?.let(::downloadRelease)
    }
    fun cancelReleasePermission() { releasePermissionFlow.cancel() }
    fun downloadRelease(item: GitHubRelease) { repository.downloadReleaseFile(item) }
}

data class HomeUiState(
    val connectionStatus: ConnectionState = Disconnected,
    val usbSessionLocked: Boolean = false,
    val firmwareAvailable: Boolean = false,
    val dialog: HomeDialog? = null,
    val releaseName: String? = null,
)
