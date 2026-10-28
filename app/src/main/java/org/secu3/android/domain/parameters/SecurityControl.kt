/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.domain.parameters

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.out.params.SecurityParamPacket
import javax.inject.Inject

data class SecurityValues(
    val useBt: Boolean,
    val useImmobilizer: Boolean,
    val btType: Int,
    val useReserveParams: Boolean,
    val checkFirmwareCrc: Boolean,
    val key0: String,
    val key1: String,
)

enum class SecurityCredentialsResult { INVALID, NOT_ADMITTED, ADMITTED }

/** Owns the received Security packet and admission of its ECU commands. */
@ViewModelScoped
class SecurityControl internal constructor(
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(parameterSession: ParameterSession) : this(parameterSession::sendPacket)

    private val mutableValues = MutableStateFlow<SecurityValues?>(null)
    val values: StateFlow<SecurityValues?> = mutableValues.asStateFlow()
    private var packet: SecurityParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: SecurityParamPacket) {
        packet = received
        publishValues()
    }

    fun changeType(value: Int): Boolean {
        if (value !in 0..5) return false
        return edit { it.btType = value }
    }

    fun changeBluetooth(value: Boolean): Boolean = edit {
        it.useBt = value
        it.btFlags = it.btFlags or (1 shl 1)
    }

    fun changeImmobilizer(value: Boolean): Boolean = edit { it.useImmobilizer = value }

    fun changeKey(index: Int, value: String): Boolean {
        if (index !in 0..1) return false
        val bytes = parseIButtonKey(value) ?: return false
        return edit { packet ->
            if (index == 0) packet.iButton0 = bytes else packet.iButton1 = bytes
        }
    }

    fun applyCredentials(name: String, password: String): SecurityCredentialsResult {
        val current = packet ?: return SecurityCredentialsResult.NOT_ADMITTED
        if (!current.useBt) return SecurityCredentialsResult.NOT_ADMITTED
        if (name.isEmpty() || password.length !in 4..8 || !password.all { it in '0'..'9' }) {
            return SecurityCredentialsResult.INVALID
        }
        val snapshot = try {
            current.credentialsSnapshot(name, password)
        } catch (_: IllegalArgumentException) {
            return SecurityCredentialsResult.INVALID
        }
        return if (sendPacket(snapshot)) SecurityCredentialsResult.ADMITTED else SecurityCredentialsResult.NOT_ADMITTED
    }

    private fun edit(change: (SecurityParamPacket) -> Unit): Boolean {
        val current = packet ?: return false
        val before = PacketFields(current)
        val admitted = try {
            change(current)
            sendPacket(current.packedSnapshot())
        } catch (_: IllegalArgumentException) {
            false
        }
        if (!admitted) {
            before.restore(current)
            return false
        }
        publishValues()
        return true
    }

    private fun publishValues() {
        val current = packet ?: return
        mutableValues.value = SecurityValues(
            useBt = current.useBt,
            useImmobilizer = current.useImmobilizer,
            btType = current.btType,
            useReserveParams = current.useReserveParams,
            checkFirmwareCrc = current.checkFwCrc,
            key0 = formatKey(current.iButton0),
            key1 = formatKey(current.iButton1),
        )
    }

    private fun formatKey(bytes: List<Int>) =
        if (bytes.size == 6) bytes.asReversed().joinToString("") { "%02X".format(it) } else ""

    fun isValidKey(value: String): Boolean = parseIButtonKey(value) != null

    private fun parseIButtonKey(value: String): List<Int>? {
        if (value.length != 12 || !value.all { it.digitToIntOrNull(16) != null }) return null
        return value.chunked(2).reversed().map { it.toInt(16) }
    }

    private data class PacketFields(
        val btName: String,
        val btPass: String,
        val btFlags: Int,
        val iButton0: List<Int>,
        val iButton1: List<Int>,
        val btType: Int,
    ) {
        constructor(packet: SecurityParamPacket) : this(
            packet.btName, packet.btPass, packet.btFlags, packet.iButton0, packet.iButton1, packet.btType,
        )

        fun restore(packet: SecurityParamPacket) {
            packet.btName = btName
            packet.btPass = btPass
            packet.btFlags = btFlags
            packet.iButton0 = iButton0
            packet.iButton1 = iButton1
            packet.btType = btType
        }
    }
}
