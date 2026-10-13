/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.ui.parameters.dialogs

import java.math.BigInteger
import java.util.Locale

internal fun parseIntegerInput(value: String, min: Int, max: Int): Int? =
    value.toIntOrNull()?.takeIf { it in min..max }

internal fun stepIntegerValue(value: String, fallback: Int, step: Int, min: Int, max: Int, direction: Int): Int {
    val base = value.toBigIntegerOrNull() ?: BigInteger.valueOf(fallback.toLong())
    val delta = BigInteger.valueOf(step.toLong()).multiply(BigInteger.valueOf(direction.toLong()))
    return base.add(delta).max(BigInteger.valueOf(min.toLong())).min(BigInteger.valueOf(max.toLong())).toInt()
}

internal fun isValidFloatInput(value: String, min: Float, max: Float): Boolean =
    value.toFloatOrNull()?.let { it in min..max } == true

internal fun stepFloatValue(value: String, fallback: Float, step: Float, min: Float, max: Float, direction: Int): Float {
    val base = value.toFloatOrNull()?.takeUnless { it.isNaN() } ?: fallback
    return (base + step * direction).coerceIn(min, max)
}

internal fun formatFloatValue(value: Float, precision: Int): String =
    String.format(Locale.US, "%.${if (precision in 0..4) precision else 4}f", value)
