package org.secu3.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.asFlow
import kotlinx.coroutines.launch
import org.secu3.android.BuildConfig
import org.secu3.android.connection.Connected
import org.secu3.android.connection.Disconnected
import org.secu3.android.connection.InProgress
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.network.models.GitHubRelease
import org.secu3.android.utils.AppPrefs
import org.secu3.android.utils.UserPrefs
import org.threeten.bp.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val manager: Secu3ConnectionManager,
    private val repository: HomeRepository,
    private val appPrefs: AppPrefs,
    val prefs: UserPrefs,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private val exitFlow = HomeExitFlow()
    private var release: GitHubRelease? = null
    private val _returnToStart = MutableStateFlow(false)
    val returnToStart = _returnToStart.asStateFlow()
    val firmware: FirmwareInfoPacket? get() = manager.fwInfo
    val isConnectionRunning get() = manager.isConnectionRunning

    init {
        viewModelScope.launch { repository.checkAndInitDb() }
        viewModelScope.launch {
            manager.connectionStateFlow.collect { status ->
                val mapped = when (status) { Connected -> HomeConnectionStatus.CONNECTED; InProgress -> HomeConnectionStatus.CONNECTING; else -> HomeConnectionStatus.DISCONNECTED }
                _uiState.value = _uiState.value.copy(
                    connectionStatus = mapped,
                    firmwareAvailable = status == Connected && (_uiState.value.firmwareAvailable || manager.fwInfo != null),
                )
                if (status == Disconnected) {
                    if (exitFlow.onDisconnected()) _returnToStart.value = true
                }
            }
        }
        viewModelScope.launch {
            val now = LocalDate.now()
            if (BuildConfig.DEBUG || appPrefs.lastAppVersionCheck.isBefore(now)) {
                release = repository.getNewRelease()
                val item = release
                if (item != null) _uiState.value = _uiState.value.copy(
                    dialog = HomeDialog.RELEASE, releaseName = item.displayName,
                )
            }
        }
        viewModelScope.launch {
            manager.firmwareLiveData.asFlow().collect {
                if (_uiState.value.connectionStatus == HomeConnectionStatus.CONNECTED && manager.isConnected) {
                    _uiState.value = _uiState.value.copy(firmwareAvailable = true)
                }
            }
        }
    }
    fun requestExit() { _uiState.value = _uiState.value.copy(dialog = HomeDialog.EXIT) }
    fun dismissDialog() { _uiState.value = _uiState.value.copy(dialog = null) }
    fun confirmExit(onStopService: () -> Unit) {
        dismissDialog()
        when (exitFlow.confirm(manager.isConnectionRunning) { onStopService() }) {
            HomeExitAction.STOP_CONNECTION -> manager.stopConnection()
            HomeExitAction.NAVIGATE_START -> _returnToStart.value = true
            HomeExitAction.NONE -> Unit
        }
    }
    fun consumeReturnToStart() {
        if (exitFlow.consumeNavigation()) _returnToStart.value = false
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
    val connectionStatus: HomeConnectionStatus = HomeConnectionStatus.DISCONNECTED,
    val firmwareAvailable: Boolean = false,
    val dialog: HomeDialog? = null,
    val releaseName: String? = null,
)
