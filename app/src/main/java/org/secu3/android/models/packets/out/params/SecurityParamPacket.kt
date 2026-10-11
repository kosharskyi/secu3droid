/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2024 Vitalii O. Kosharskyi. Ukraine, Kyiv
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
package org.secu3.android.models.packets.out.params

import org.secu3.android.models.packets.base.Secu3Packet
import org.secu3.android.models.packets.base.InputPacket
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.utils.getBitValue
import org.secu3.android.utils.setBitValue
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class SecurityParamPacket(

    var btName: String = "",    // max length 8 character
    var btPass: String = "",    // max length 8 character

    var btFlags: Int = 0,

    var iButton0: List<Int> = emptyList(),
    var iButton1: List<Int> = emptyList(),

    var btType: Int = 0 // Bluetooth chip type: 0 - BC417, 1 - BK3231, 2 - BK3231S(JDY-31), 3 - BC352(HC-05), 4 - BK3432, 5 - BK3431S

) : Secu3Packet(), InputPacket, OutputPacket {

    var useBt: Boolean                                  // specifies to use or not to use bluetooth
        get() = btFlags.getBitValue(0) > 0
        set(value) {
            btFlags = btFlags.setBitValue(value, 0)
        }

    val setBtBaudRate: Boolean                          // indicates that bluetooth baud rate has to be set during start up
        get() = btFlags.getBitValue(1) > 0

    var useImmobilizer: Boolean                         // specifies to use or not to use immobilizer
        get() = btFlags.getBitValue(2) > 0
        set(value) {
            btFlags = btFlags.setBitValue(value, 2)
        }

    val useReserveParams: Boolean                       // Use reserve parameters instead of parameters stored in the EEPROM
        get() = btFlags.getBitValue(3) > 0

    val checkFwCrc: Boolean                             //Check firmware CRC (time consuming operation)
        get() = btFlags.getBitValue(4) > 0

    fun packedSnapshot(): OutputPacket {
        val bytes = pack().copyOf()
        return object : OutputPacket { override fun pack(): IntArray = bytes.copyOf() }
    }

    fun credentialsSnapshot(name: String, password: String): OutputPacket {
        btName = name
        btPass = password
        return try { packedSnapshot() } finally {
            btName = ""
            btPass = ""
        }
    }

    override fun pack(): IntArray {
        var data = intArrayOf(DESCRIPTOR.code)

        val credentialsProvided = btName.isNotEmpty() || btPass.isNotEmpty()
        val nameBytes = if (credentialsProvided) encodeManagerText(btName) else byteArrayOf()
        val passBytes = if (credentialsProvided) btPass.toByteArray(StandardCharsets.US_ASCII) else byteArrayOf()
        require(btName.length <= 16 && nameBytes.size <= 16) { "Bluetooth name must fit in 16 OEM bytes" }
        if (credentialsProvided) require(btName.isNotEmpty() && passBytes.size in 4..8 && btPass.all { it in '0'..'9' }) { "Bluetooth credentials require a name and a 4–8 digit password" }
        require(iButton0.size == IBTN_KEY_SIZE && iButton1.size == IBTN_KEY_SIZE) { "Each iButton key must contain 6 bytes" }
        require((iButton0 + iButton1).all { it in 0..255 }) { "Invalid iButton key byte" }

        if (credentialsProvided) {
            data += nameBytes.size
            data += passBytes.size
            data += nameBytes.map { it.toInt() and 0xff }.toIntArray()
            data += passBytes.map { it.toInt() and 0xff }.toIntArray()
        } else {
            data += 0
            data += 0
        }

        data += btFlags

        data += iButton0
        data += iButton1

        data += btType

        data += unhandledParams

        return data
    }

    private fun encodeManagerText(value: String): ByteArray {
        try {
            val encoder = Charset.forName("IBM866").newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            val buffer = encoder.encode(java.nio.CharBuffer.wrap(value))
            return ByteArray(buffer.remaining()).also { buffer.get(it) }
        } catch (error: java.nio.charset.CharacterCodingException) {
            throw IllegalArgumentException("Bluetooth name contains characters unsupported by the ECU OEM encoding", error)
        }
    }

    override fun parse(data: IntArray): InputPacket {
        //Number of characters in name (must be zero)
        val numNam = data.get1Byte()
        if (numNam > 0) {
            throw IllegalArgumentException("Bt name is not empty")
        }

        //Number of characters in password (must be zero)
        val numPass = data.get1Byte()
        if (numPass > 0) {
            throw IllegalArgumentException("Bt password is not empty")
        }

        btFlags = data.get1Byte()

        iButton0 = emptyList()
        iButton1 = emptyList()

        for (i in 0 until IBTN_KEY_SIZE) {
            iButton0 += data.get1Byte()
        }

        for (i in 0 until IBTN_KEY_SIZE) {
            iButton1 += data.get1Byte()
        }

        btType = data.get1Byte()

        data.setUnhandledParams()

        return this
    }

    companion object {

        internal const val DESCRIPTOR = '#'

        private const val IBTN_KEY_SIZE = 6
    }
}
