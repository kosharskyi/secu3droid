/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitaliy O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2024 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.utils

import java.util.ArrayList

object PacketUtils {

    // There are several special reserved symbols in binary mode: 0x21, 0x40,
    // 0x0D, 0x0A
    const val FIBEGIN = 0x21 // '!' indicates beginning of the

    // ingoing packet
    private const val FOBEGIN = 0x40 // '@' indicates beginning of the

    // outgoing packet
    private const val FIOEND = 0x0D // '\r' indicates ending of the

    // ingoing/outgoing packet
    private const val FESC = 0x0A // '\n' Packet escape (FESC)

    // Following bytes are used only in escape sequeces and may appear in the
    // data without any problems
    private const val TFIBEGIN = 0x81 // Transposed FIBEGIN

    private const val TFOBEGIN = 0x82 // Transposed FOBEGIN

    private const val TFIOEND = 0x83 // Transposed FIOEND

    private const val TFESC = 0x84 // Transposed FESC

    fun EscRxPacket(packetBuffer: IntArray): IntArray {
        val buf = IntArray(packetBuffer.size)
        var esc = false
        var idx = 0
        for (i in packetBuffer.indices) {
            if (packetBuffer[i] == FESC && i >= 2) {
                esc = true
                continue
            }
            if (esc) {
                esc = false
                when(packetBuffer[i]) {
                    TFOBEGIN -> buf[idx++] = FOBEGIN
                    TFIOEND -> buf[idx++] = FIOEND
                    TFESC -> buf[idx++] = FESC
                }
            } else buf[idx++] = packetBuffer[i]
        }
        return buf.sliceArray(IntRange(0, idx - 1))
    }

    fun EscTxPacket(packetBuffer: IntArray): IntArray {
        val buf = ArrayList<Int>(packetBuffer.size - 3)
        for (i in packetBuffer.indices) {
            if (i >= 2 && i < packetBuffer.size - 1) {
                if (packetBuffer[i] == FIBEGIN) {
                    buf.add(FESC)
                    buf.add(TFIBEGIN)
                    continue
                } else if (packetBuffer[i] == FIOEND) {
                    buf.add(FESC)
                    buf.add(TFIOEND)
                    continue
                } else if (packetBuffer[i] == FESC) {
                    buf.add(FESC)
                    buf.add(TFESC)
                    continue
                }
            }
            buf.add(packetBuffer[i])
        }
        val outBuf = IntArray(buf.size)
        for (i in buf.indices) {
            outBuf[i] = buf[i]
        }
        return outBuf
    }

    fun calculateChecksum(packet: IntArray): UByteArray {
        val crc22 = UByteArray(2) { 0u }

        for (i in packet) {
            crc22[0] = (crc22[0] + i.toUByte()).toUByte()
            crc22[1] = (crc22[1] + crc22[0]).toUByte()
        }

        crc22[0] = (crc22[0] + packet.size.toUByte()).toUByte()
        crc22[1] = (crc22[1] + crc22[0]).toUByte()

        return crc22
    }
}