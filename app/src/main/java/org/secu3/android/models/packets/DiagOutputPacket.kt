/* SecuDroid  - An open source, free manager for SECU-3 engine control unit
   Copyright (C) 2020 Vitaliy O. Kosharskiy. Ukraine, Kharkiv

   SECU-3  - An open source, free engine control unit
   Copyright (C) 2007 Alexey A. Shabelnikov. Ukraine, Kyiv

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

   contacts:
              http://secu-3.org
              email: vetalkosharskiy@gmail.com
*/
package org.secu3.android.models.packets

class DiagOutputPacket(private val fwInfo: FirmwareInfoPacket) : BaseOutputPacket() {

    private var out: Int = 0
    private var frq: Int = 0
    private var duty: Int = 0
    private var chan: Int = 0




    var ignOut1: Boolean
        get() = out.getBitValue(0) > 0
        set(value) {
            out = if (value) {
                1 or out
            } else {
                1.inv().and(out)
            }
        }

    var ignOut2: Boolean
        get() = out.getBitValue(1) > 0
        set(value) {
            out = if (value) {
                (1 shl 1).or(out)
            } else {
                (1 shl 1).inv().and(out)
            }
        }

    var ignOut3: Boolean
        get() = out.getBitValue(2) > 0
        set(value) {
            out = if (value) {
                (1 shl 2).or(out)
            } else {
                (1 shl 2).inv().and(out)
            }
        }

    var ignOut4: Boolean
        get() = out.getBitValue(3) > 0
        set(value) {
            out = if (value) {
                (1 shl 3).or(out)
            } else {
                (1 shl 3).inv().and(out)
            }
        }

    var ignOut5: Boolean
        get() = out.getBitValue(4) > 0
        set(value) {
            out = if (value) {
                (1 shl 4).or(out)
            } else {
                (1 shl 4).inv().and(out)
            }
        }

    var ie: Boolean
        get() = out.getBitValue(4) > 0
        set(value) {
            out = if (value) {
                (1 shl 4).or(out)
            } else {
                (1 shl 4).inv().and(out)
            }
        }

    var fe: Boolean
        get() = out.getBitValue(5) > 0
        set(value) {
            out = if (value) {
                (1 shl 5).or(out)
            } else {
                (1 shl 5).inv().and(out)
            }
        }



    private val ecfBitPosition: Int
        get() = if (fwInfo.isSecu3T) 6 else 5

    var ecf: Boolean
        get() = out.getBitValue(ecfBitPosition) > 0
        set(value) {
            out = if (value) {
                (1 shl ecfBitPosition).or(out)
            } else {
                (1 shl ecfBitPosition).inv().and(out)
            }
        }

    var ce: Boolean
        get() = out.getBitValue(7) > 0
        set(value) {
            out = if (value) {
                (1 shl 7).or(out)
            } else {
                (1 shl 7).inv().and(out)
            }
        }

    var stBlock: Boolean
        get() = out.getBitValue(8) > 0
        set(value) {
            out = if (value) {
                (1 shl 8).or(out)
            } else {
                (1 shl 8).inv().and(out)
            }
        }

    var addIo1: Boolean
        get() = out.getBitValue(9) > 0
        set(value) {
            out = if (value) {
                (1 shl 9).or(out)
            } else {
                (1 shl 9).inv().and(out)
            }
        }

    var addIo2: Boolean
        get() = out.getBitValue(10) > 0
        set(value) {
            out = if (value) {
                (1 shl 10).or(out)
            } else {
                (1 shl 10).inv().and(out)
            }
        }





    var injO1: Boolean
        get() = out.getBitValue(6) > 0
        set(value) {
            out = if (value) {
                (1 shl 6).or(out)
            } else {
                (1 shl 6).inv().and(out)
            }
        }

    var injO2: Boolean
        get() = out.getBitValue(7) > 0
        set(value) {
            out = if (value) {
                (1 shl 7).or(out)
            } else {
                (1 shl 7).inv().and(out)
            }
        }

    var injO3: Boolean
        get() = out.getBitValue(8) > 0
        set(value) {
            out = if (value) {
                (1 shl 8).or(out)
            } else {
                (1 shl 8).inv().and(out)
            }
        }

    var injO4: Boolean
        get() = out.getBitValue(9) > 0
        set(value) {
            out = if (value) {
                (1 shl 9).or(out)
            } else {
                (1 shl 9).inv().and(out)
            }
        }

    var injO5: Boolean
        get() = out.getBitValue(10) > 0
        set(value) {
            out = if (value) {
                (1 shl 10).or(out)
            } else {
                (1 shl 10).inv().and(out)
            }
        }




    var enableBlDeTesting: Boolean
        get() = out.getBitValue(12) > 0
        set(value) {
            if (value) {
                out = (1 shl 12).or(out)
                out = (1 shl 14).or(out)
            } else {
                out = (1 shl 12).inv().and(out)
                out = (1 shl 14).inv().and(out)
            }
        }

    var bl: Boolean
        get() = out.getBitValue(11) > 0
        set(value) {
            out = if (value) {
                (1 shl 11).or(out)
            } else {
                (1 shl 11).inv().and(out)
            }
        }

    var de: Boolean
        get() = out.getBitValue(13) > 0
        set(value) {
            out = if (value) {
                (1 shl 13).or(out)
            } else {
                (1 shl 13).inv().and(out)
            }
        }





    var stblO: Boolean
        get() = out.getBitValue(15) > 0
        set(value) {
            out = if (value) {
                (1 shl 15).or(out)
            } else {
                (1 shl 15).inv().and(out)
            }
        }

    var celO: Boolean
        get() = out.getBitValue(16) > 0
        set(value) {
            out = if (value) {
                (1 shl 16).or(out)
            } else {
                (1 shl 16).inv().and(out)
            }
        }

    var fpmpO: Boolean
        get() = out.getBitValue(17) > 0
        set(value) {
            out = if (value) {
                (1 shl 17).or(out)
            } else {
                (1 shl 17).inv().and(out)
            }
        }

    var pwrrO: Boolean
        get() = out.getBitValue(18) > 0
        set(value) {
            out = if (value) {
                (1 shl 18).or(out)
            } else {
                (1 shl 18).inv().and(out)
            }
        }

    var evapO: Boolean
        get() = out.getBitValue(19) > 0
        set(value) {
            out = if (value) {
                (1 shl 19).or(out)
            } else {
                (1 shl 19).inv().and(out)
            }
        }

    var o2shO: Boolean
        get() = out.getBitValue(20) > 0
        set(value) {
            out = if (value) {
                (1 shl 20).or(out)
            } else {
                (1 shl 20).inv().and(out)
            }
        }

    var condO: Boolean
        get() = out.getBitValue(21) > 0
        set(value) {
            out = if (value) {
                (1 shl 21).or(out)
            } else {
                (1 shl 21).inv().and(out)
            }
        }

    var addO2: Boolean
        get() = out.getBitValue(22) > 0
        set(value) {
            out = if (value) {
                (1 shl 22).or(out)
            } else {
                (1 shl 22).inv().and(out)
            }
        }


    var tachO: Boolean
        get() = out.getBitValue(23) > 0
        set(value) {
            out = if (value) {
                (1 shl 23).or(out)
            } else {
                (1 shl 23).inv().and(out)
            }
        }


    var enableTachOtesting: Boolean
        get() = out.getBitValue(24) > 0
        set(value) {
            out = if (value) {
                (1 shl 24).or(out)
            } else {
                (1 shl 24).inv().and(out)
            }
        }


    override fun pack(): String {
        var data = "$OUTPUT_PACKET_SYMBOL$DESCRIPTOR"

        data += data.write4Bytes(out)
//        data += data.write2Bytes(frq)

//        data += duty.toChar()
//        data += chan.toChar()

        return data
    }

    companion object {

        internal const val DESCRIPTOR = '^'

    }


}
