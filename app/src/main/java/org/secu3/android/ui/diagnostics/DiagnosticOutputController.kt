package org.secu3.android.ui.diagnostics

import org.secu3.android.models.packets.out.DiagOutputPacket

internal class DiagnosticOutputController(
    val packet: DiagOutputPacket,
    private val send: () -> Unit,
) {
    fun setOutput(output: DiagnosticOutput, enabled: Boolean) {
        setDiagnosticOutput(packet, output, enabled)
        send()
    }

    fun selectChannel(index: Int) = selectDiagnosticOutputChannel(packet, index, send)
    fun editFrequency(value: Float) = updateDiagnosticOutputFrequency(packet, value, send)
    fun editDuty(value: Float) = updateDiagnosticOutputDuty(packet, value, send)

    fun enableBlDe(enabled: Boolean) {
        if (!enabled) {
            packet.bl = false
            packet.de = false
            send()
        }
    }

    fun enableTachO(enabled: Boolean) {
        if (!enabled) {
            packet.tachO = false
            send()
        }
    }

    fun applyAdditionalOptions(enableBlDe: Boolean, enableTachO: Boolean) {
        // Keep legacy ordering and separate sends for each disabled option.
        enableBlDe(enableBlDe)
        enableTachO(enableTachO)
    }
}
