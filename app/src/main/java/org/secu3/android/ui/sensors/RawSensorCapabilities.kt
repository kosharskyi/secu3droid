package org.secu3.android.ui.sensors

import org.secu3.android.models.packets.input.FirmwareInfoPacket

/** Firmware-dependent availability of the additional raw ADC channels. Null means unknown. */
data class RawSensorCapabilities(
    val isSecu3T: Boolean? = null,
    val isTpic8101Enabled: Boolean? = null,
    val isAtmega1284: Boolean? = null,
) {
    fun isChannelVisible(channel: Int): Boolean = when {
        channel == 3 || channel in 5..8 -> isSecu3T != true
        else -> true
    }

    fun isChannelSupported(channel: Int): Boolean = when (channel) {
        3 -> isSecu3T != true
        4 -> isTpic8101Enabled != false
        in 5..8 -> isAtmega1284 != false && isSecu3T != true
        else -> true
    }

    companion object {
        val Unknown = RawSensorCapabilities()

        fun from(firmware: FirmwareInfoPacket?): RawSensorCapabilities = firmware?.let {
            RawSensorCapabilities(
                isSecu3T = it.isSecu3T,
                isTpic8101Enabled = it.isTPIC8101,
                isAtmega1284 = it.isATMEGA1284,
            )
        } ?: Unknown
    }
}
