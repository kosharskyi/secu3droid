package org.secu3.android.domain.ltft

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LtftMapExport @Inject constructor() {
    fun tsv(corrections: List<Float>, rows: IntRange, columns: IntRange, locale: Locale): String =
        buildString { appendRows(corrections, rows, columns, '\t', locale) }

    fun csv(corrections: List<Float>, rpmAxis: List<Int>, decimalSeparator: Char): String = buildString {
        val axis = 0 until LtftMapsSession.AXIS_SIZE
        val locale = if (decimalSeparator == ',') Locale.GERMANY else Locale.ROOT
        appendRows(corrections, axis, axis, ';', locale)
        append(rpmAxis.joinToString(";"))
    }

    suspend fun writeCsv(resolver: ContentResolver, uri: Uri, csv: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val stream = resolver.openOutputStream(uri, "wt") ?: return@withContext false
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun StringBuilder.appendRows(
        corrections: List<Float>,
        rows: IntRange,
        columns: IntRange,
        separator: Char,
        locale: Locale,
    ) {
        for (row in rows.last downTo rows.first) {
            for (column in columns) {
                if (column != columns.first) append(separator)
                append(String.format(locale, "%.1f", corrections[row * LtftMapsSession.AXIS_SIZE + column]))
            }
            append("\r\n")
        }
    }
}
