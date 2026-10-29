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
import org.secu3.android.connection.Secu3ConnectionManager
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.DbwParamPacket
import javax.inject.Inject

data class DbwValues(
    val etcP: Float,
    val etcI: Float,
    val etcD: Float,
    val negativeMaxDuty: Float,
    val positiveMaxDuty: Float,
    val pidPeriod: Float,
    val frictionTorqueOpen: Float,
    val frictionTorqueClose: Float,
    val frictionThreshold: Float,
    val frictionIdleAdditionMax: Float,
    val homePosition: Float,
) {
    companion object {
        fun from(packet: DbwParamPacket) = DbwValues(
            packet.etc_p, packet.etc_i, packet.etc_d, packet.etc_nmax_duty,
            packet.etc_pmax_duty, packet.pid_period, packet.frictorq_open,
            packet.frictorq_close, packet.frictorq_thrd,
            packet.frictorq_idleadd_max, packet.homePosition_edited,
        )
    }
}

/** Owns DBW packet edits while preserving the receive-only home position and unknown tail. */
@ViewModelScoped
class DbwControl internal constructor(
    private val firmware: () -> FirmwareInfoPacket?,
    private val sendPacket: (OutputPacket) -> Boolean,
) {
    @Inject
    constructor(connectionManager: Secu3ConnectionManager, parameterSession: ParameterSession) : this(
        firmware = { connectionManager.fwInfo },
        sendPacket = parameterSession::sendPacket,
    )

    private val mutableValues = MutableStateFlow<DbwValues?>(null)
    val values: StateFlow<DbwValues?> = mutableValues.asStateFlow()
    private var packet: DbwParamPacket? = null

    fun reset() {
        packet = null
        mutableValues.value = null
    }

    fun receive(received: DbwParamPacket) {
        packet = received
        publishValues()
    }

    fun editEtcP(value: Float) = editFloat(value, 0f, 5f) { it.etc_p = value }
    fun editEtcI(value: Float) = editFloat(value, 0f, 5f) { it.etc_i = value }
    fun editEtcD(value: Float) = editFloat(value, 0f, 5f) { it.etc_d = value }
    fun editNegativeMaxDuty(value: Float) = editFloat(value, 0f, 100f) { it.etc_nmax_duty = value }
    fun editPositiveMaxDuty(value: Float) = editFloat(value, 0f, 100f) { it.etc_pmax_duty = value }
    fun editPidPeriod(value: Float) = editFloat(value, 0f, .2f) { it.pid_period = value }
    fun editFrictionTorqueOpen(value: Float) = editFloat(value, 0f, 15.9f) { it.frictorq_open = value }
    fun editFrictionTorqueClose(value: Float) = editFloat(value, 0f, 15.9f) { it.frictorq_close = value }
    fun editFrictionThreshold(value: Float) = editFloat(value, 0f, 25f) { it.frictorq_thrd = value }
    fun editFrictionIdleAdditionMax(value: Float) = editFloat(value, 0f, 15f) { it.frictorq_idleadd_max = value }

    private inline fun editFloat(value: Float, min: Float, max: Float, change: (DbwParamPacket) -> Unit): Boolean =
        edit(value.isFinite() && value in min..max, change)

    private inline fun edit(valid: Boolean, change: (DbwParamPacket) -> Unit): Boolean {
        if (!valid || !ParameterCapabilities(firmware()).dbw) return false
        val current = packet ?: return false
        val before = DbwFields(current)
        val admitted = try {
            change(current)
            val bytes = current.pack().copyOf()
            sendPacket(object : OutputPacket {
                override fun pack(): IntArray = bytes.copyOf()
            })
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
        packet?.let { mutableValues.value = DbwValues.from(it) }
    }

    private data class DbwFields(
        val p: Float, val i: Float, val d: Float, val nmax: Float, val pmax: Float,
        val period: Float, val open: Float, val close: Float, val threshold: Float,
        val idleAddition: Float, val homePosition: Float,
    ) {
        constructor(packet: DbwParamPacket) : this(
            packet.etc_p, packet.etc_i, packet.etc_d, packet.etc_nmax_duty,
            packet.etc_pmax_duty, packet.pid_period, packet.frictorq_open,
            packet.frictorq_close, packet.frictorq_thrd,
            packet.frictorq_idleadd_max, packet.homePosition_edited,
        )

        fun restore(packet: DbwParamPacket) {
            packet.etc_p = p
            packet.etc_i = i
            packet.etc_d = d
            packet.etc_nmax_duty = nmax
            packet.etc_pmax_duty = pmax
            packet.pid_period = period
            packet.frictorq_open = open
            packet.frictorq_close = close
            packet.frictorq_thrd = threshold
            packet.frictorq_idleadd_max = idleAddition
            packet.homePosition_edited = homePosition
        }
    }
}
