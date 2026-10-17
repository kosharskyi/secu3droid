package org.secu3.android.ui.diagnostics

import org.secu3.android.models.packets.out.DiagOutputPacket

internal class DiagnosticOutputController(
    val packet: DiagOutputPacket,
    private val send: () -> Unit,
) {
    fun setOutput(output: DiagnosticOutput, enabled: Boolean) {
        if (isDisabledSpecialOutput(output)) return
        setDiagnosticOutput(packet, output, enabled)
        send()
    }

    fun selectChannel(index: Int) = selectDiagnosticOutputChannel(packet, index, send)
    fun editFrequency(value: Float) = updateDiagnosticOutputFrequency(packet, value, send)
    fun editDuty(value: Float) = updateDiagnosticOutputDuty(packet, value, send)

    fun enableBlDe(enabled: Boolean) {
        packet.bl = false
        packet.de = false
        packet.blOutputEnabled = enabled
        packet.deOutputEnabled = enabled
        send()
    }

    fun enableTachO(enabled: Boolean) {
        packet.tachO = false
        packet.tachOOutputEnabled = enabled
        send()
    }

    fun applyAdditionalOptions(enableBlDe: Boolean, enableTachO: Boolean) {
        // Match the desktop's reset-and-send order for each additional output option.
        enableBlDe(enableBlDe)
        enableTachO(enableTachO)
    }

    private fun isDisabledSpecialOutput(output: DiagnosticOutput) = when (output) {
        DiagnosticOutput.BL -> !packet.blOutputEnabled
        DiagnosticOutput.DE -> !packet.deOutputEnabled
        DiagnosticOutput.TACH_O -> !packet.tachOOutputEnabled
        else -> false
    }
}
