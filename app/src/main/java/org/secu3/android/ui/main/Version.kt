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

package org.secu3.android.ui.main

data class Version(val major: Int, val minor: Int, val patch: Int = 0) : Comparable<Version> {
    companion object {
        fun parse(version: String): Version {
            val parts = version.split(".")
            require(parts.size in 2..3) { "Version must have two or three parts: major.minor[.patch]" }
            val major = parts[0].toInt()
            val minor = parts[1].toInt()
            val patch = if (parts.size == 3) parts[2].toInt() else 0
            return Version(major, minor, patch)
        }
    }

    override fun compareTo(other: Version): Int {
        return when {
            this.major != other.major -> this.major - other.major
            this.minor != other.minor -> this.minor - other.minor
            else -> this.patch - other.patch
        }
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }
}
