package org.secu3.android.connection

import java.util.Locale

/** Shared normalization for Bluetooth identities stored in preferences and exposed by Android. */
internal object BluetoothDeviceAddress {
    fun normalize(value: String?): String? {
        val address = value?.trim()?.uppercase(Locale.ROOT) ?: return null
        return address.takeIf { ADDRESS_PATTERN.matches(it) }
    }

    fun matches(first: String?, second: String?): Boolean =
        normalize(first) != null && normalize(first) == normalize(second)

    fun <T> find(address: String?, candidates: Iterable<T>, candidateAddress: (T) -> String?): T? {
        val normalized = normalize(address) ?: return null
        return candidates.firstOrNull { matches(candidateAddress(it), normalized) }
    }

    private val ADDRESS_PATTERN = Regex("(?:[0-9A-F]{2}:){5}[0-9A-F]{2}")
}
