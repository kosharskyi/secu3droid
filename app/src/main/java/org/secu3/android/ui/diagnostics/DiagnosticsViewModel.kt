/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.domain.diagnostics.DiagnosticOutput
import org.secu3.android.domain.diagnostics.DiagnosticsSession
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    connectionManager: Secu3ConnectionManager,
    private val session: DiagnosticsSession,
) : ViewModel() {
    val state = session.state
    val connectionState = connectionManager.connectionStateFlow
    val firmwareInfo = connectionManager.firmwareInfo

    init {
        session.start(viewModelScope)
    }

    fun setOutput(output: DiagnosticOutput, enabled: Boolean) = session.setOutput(output, enabled)
    fun selectChannel(index: Int) = session.selectChannel(index)
    fun editFrequency(value: Float) = session.editFrequency(value)
    fun editDuty(value: Float) = session.editDuty(value)
    fun applyAdditionalOptions(enableBlDe: Boolean, enableTachO: Boolean) =
        session.applyAdditionalOptions(enableBlDe, enableTachO)

    override fun onCleared() = session.close()
}
