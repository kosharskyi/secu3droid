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

import android.content.Context
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.TypedValueCompat
import androidx.fragment.app.Fragment
import retrofit2.Response
import java.io.File
import java.text.DecimalFormat
import kotlin.math.roundToInt


fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun ImageView.animateRotationByX(xDegrees: Float) {
    this.animate()
            .rotationX(xDegrees)
            .setDuration(400)
            .interpolator = AccelerateDecelerateInterpolator()
}

fun Fragment.hideKeyboard(): Boolean {
    activity?.currentFocus?.let {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
        return true
    }
    return false
}

fun AppCompatActivity.hideKeyboard(): Boolean {
    currentFocus?.let {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
        return true
    }
    return false
}

fun File.sizeStr(): String {
    val size = length()
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var unitIndex = 0

    var sizeInUnits = size.toDouble()
    while (sizeInUnits > 1024 && unitIndex < units.size - 1) {
        sizeInUnits /= 1024.0
        unitIndex++
    }

    val df = DecimalFormat("#.##")
    return "${df.format(sizeInUnits)} ${units[unitIndex]}"
}

fun Int.getBitValue(bitNumber: Int): Int {
    return this shr bitNumber and 1
}

fun Int.setBitValue(value: Boolean, bitNumber: Int): Int {
    return if (value) {
        1.shl(bitNumber).or(this)
    } else {
        1.shl(bitNumber).inv().and(this)
    }
}

fun Context.dpToPx(dp: Int): Int {
    return TypedValueCompat.dpToPx(dp.toFloat(), resources.displayMetrics).roundToInt()
}

fun Context.pxToDp(px: Int): Int {
    return TypedValueCompat.pxToDp(px.toFloat(), resources.displayMetrics).roundToInt()
}

fun <T : Any> Response<T>.toResult(): Result<T> {
    return try {
        if (isSuccessful) {
            val body = body()
            if (body != null) {
                Result.success(body)
            } else {
                Result.failure(NullPointerException("Response body is null"))
            }
        } else {
            val errorBody = errorBody()?.string()
            val message = errorBody ?: message()
            Result.failure(Throwable("Unsuccessful response: $message"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}