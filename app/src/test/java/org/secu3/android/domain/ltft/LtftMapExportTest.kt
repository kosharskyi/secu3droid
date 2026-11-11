package org.secu3.android.domain.ltft

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LtftMapExportTest {
    private val export = LtftMapExport()
    private val corrections = List(256) { it / 16 + (it % 16) / 10f - 8f }
    private val rpm = List(16) { 500 * (it + 1) }

    @Test
    fun copiesOnlySelectedCellsWithHighLoadFirst() {
        assertEquals("6.2\t6.3\r\n5.2\t5.3\r\n", export.tsv(corrections, 13..14, 2..3, Locale.ROOT))
        assertEquals("-7,5\r\n", export.tsv(corrections, 0..0, 5..5, Locale.GERMANY))
    }

    @Test
    fun csvHasSixteenCorrectionRowsThenTheRpmAxisWithNoTrailingLineBreak() {
        val csv = export.csv(corrections, rpm, '.')
        val rows = csv.split("\r\n")

        assertEquals(List(17) { 16 }, rows.map { it.split(';').size })
        assertEquals("7.0;7.1;7.2;7.3;7.4;7.5;7.6;7.7;7.8;7.9;8.0;8.1;8.2;8.3;8.4;8.5", rows.first())
        assertEquals("-8.0;-7.9;-7.8;-7.7;-7.6;-7.5;-7.4;-7.3;-7.2;-7.1;-7.0;-6.9;-6.8;-6.7;-6.6;-6.5", rows[15])
        assertEquals("500;1000;1500;2000;2500;3000;3500;4000;4500;5000;5500;6000;6500;7000;7500;8000", rows.last())
        assertEquals(csv.replace('.', ','), export.csv(corrections, rpm, ','))
    }
}
