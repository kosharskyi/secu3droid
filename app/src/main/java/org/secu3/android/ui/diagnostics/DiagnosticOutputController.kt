package org.secu3.android.ui.diagnostics

import org.secu3.android.models.packets.out.DiagOutputPacket

internal class DiagnosticOutputController(
    val packet: DiagOutputPacket,
    private val send: () -> Unit,
) {
    private var initialized = false
    private var initialStateSent = false
    private var firmwareFamilyIsSecu3T: Boolean? = null

    fun initializeIgnitionOutputs(isSecu3T: Boolean) {
        if (initialized) return
        firmwareFamilyIsSecu3T = isSecu3T
        packet.out = (packet.out and IGNITION_OUTPUT_MASK.inv()) or if (isSecu3T) IGNITION_OUTPUT_MASK else 0
        initialized = true
    }

    fun onDiagnosticInput(isSecu3T: Boolean) {
        initializeIgnitionOutputs(isSecu3T)
        if (initialStateSent) return
        initialStateSent = true
        send()
    }

    fun setOutput(output: DiagnosticOutput, enabled: Boolean) {
        if (diagnosticOutputForChannel(firmwareFamilyIsSecu3T, packet.chan) == output) return
        if (isDisabledSpecialOutput(output)) return
        setDiagnosticOutput(packet, output, enabled)
        sendIfInitialized()
    }

    fun selectChannel(index: Int) = selectDiagnosticOutputChannel(packet, index, ::sendIfInitialized)
    fun editFrequency(value: Float) = updateDiagnosticOutputFrequency(packet, value, ::sendIfInitialized)
    fun editDuty(value: Float) = updateDiagnosticOutputDuty(packet, value, ::sendIfInitialized)

    fun enableBlDe(enabled: Boolean) {
        if (packet.blOutputEnabled == enabled && packet.deOutputEnabled == enabled) return
        packet.bl = false
        packet.de = false
        packet.blOutputEnabled = enabled
        packet.deOutputEnabled = enabled
        sendIfInitialized()
    }

    fun enableTachO(enabled: Boolean) {
        if (packet.tachOOutputEnabled == enabled) return
        packet.tachO = false
        packet.tachOOutputEnabled = enabled
        sendIfInitialized()
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

    private fun sendIfInitialized() {
        if (initialized) send()
    }

    private companion object {
        const val IGNITION_OUTPUT_MASK = 0b11
    }
}
