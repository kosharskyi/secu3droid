package org.secu3.android.db

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.secu3.android.db.models.GaugeState
import org.secu3.android.models.sensors.GaugeType
import org.secu3.android.models.sensors.IndicatorType
import java.util.UUID

class AppDatabaseInitializationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun firstOpenSeedsDefaultGaugeAndIndicatorOrder() = withDatabase { database ->
        runBlocking {
            val gauges = database.gaugeStateDao().getAllOrderByIdx()
            assertEquals(
                listOf(GaugeType.RPM, GaugeType.MAP, GaugeType.VOLTAGE, GaugeType.TEMPERATURE),
                gauges.map { it.gaugeType },
            )
            assertEquals(listOf(0, 1, 2, 3), gauges.map { it.idx })
            assertEquals(listOf(false, false, false, false), gauges.map { it.isNumericView })

            val indicators = database.indicatorStateDao().getAllOrderByIdx()
            assertEquals(
                listOf(
                    IndicatorType.GAS_VALVE,
                    IndicatorType.THROTTLE,
                    IndicatorType.FI_FUEL,
                    IndicatorType.POWER_VALVE,
                    IndicatorType.STARTER_BLOCKING,
                    IndicatorType.AE,
                    IndicatorType.COOLING_FAN,
                    IndicatorType.CHECK_ENGINE,
                    IndicatorType.REV_LIM_FUEL_CUT,
                    IndicatorType.FLOOD_CLEAR_MODE,
                ),
                indicators.map { it.indicatorType },
            )
            assertEquals((0..9).toList(), indicators.map { it.idx })
        }
    }

    @Test
    fun reopeningKeepsConfiguredRowsWithoutReinsertingDefaults() = withDatabaseFile { name ->
        withOpenedDatabase(openDatabase(name)) { database ->
            runBlocking {
                database.gaugeStateDao().deleteGauge(GaugeType.MAP)
                database.gaugeStateDao().insert(GaugeState(
                    gaugeType = GaugeType.OIL_PRESSURE,
                    idx = 3,
                    isNumericView = true,
                ))
                database.indicatorStateDao().deleteIndicator(IndicatorType.THROTTLE)
            }
        }

        withOpenedDatabase(openDatabase(name)) { database ->
            runBlocking {
                val gauges = database.gaugeStateDao().getAllOrderByIdx()
                assertEquals(
                    listOf(GaugeType.RPM, GaugeType.VOLTAGE, GaugeType.TEMPERATURE, GaugeType.OIL_PRESSURE),
                    gauges.map { it.gaugeType },
                )
                assertEquals(listOf(0, 1, 2, 3), gauges.map { it.idx })
                assertEquals(listOf(false, false, false, true), gauges.map { it.isNumericView })
                assertEquals(9, database.indicatorStateDao().getAll().size)
                assertEquals(null, database.indicatorStateDao().getByIndicatorType(IndicatorType.THROTTLE))
            }
        }
    }

    @Test
    fun reopeningAfterAllCardsAreDeletedKeepsTablesEmpty() = withDatabaseFile { name ->
        withOpenedDatabase(openDatabase(name)) { database ->
            runBlocking {
                database.gaugeStateDao().getAll().forEach(database.gaugeStateDao()::delete)
                database.indicatorStateDao().getAll().forEach(database.indicatorStateDao()::delete)
            }
        }

        withOpenedDatabase(openDatabase(name)) { database ->
            runBlocking {
                assertEquals(emptyList<Any>(), database.gaugeStateDao().getAll())
                assertEquals(emptyList<Any>(), database.indicatorStateDao().getAll())
            }
        }
    }

    @Test
    fun openingLegacyDatabaseDoesNotSeedIt() = withDatabaseFile { name ->
        withOpenedDatabase(Room.databaseBuilder(context, AppDatabase::class.java, name).build()) { legacyDatabase ->
            runBlocking {
                assertEquals(emptyList<Any>(), legacyDatabase.gaugeStateDao().getAll())
                assertEquals(emptyList<Any>(), legacyDatabase.indicatorStateDao().getAll())
            }
        }

        withOpenedDatabase(openDatabase(name)) { database ->
            runBlocking {
                assertEquals(emptyList<Any>(), database.gaugeStateDao().getAll())
                assertEquals(emptyList<Any>(), database.indicatorStateDao().getAll())
            }
        }
    }

    private fun openDatabase(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .createFromAsset("database/secu3droid.db")
            .build()

    private fun withDatabase(block: (AppDatabase) -> Unit) = withDatabaseFile { name ->
        withOpenedDatabase(openDatabase(name), block)
    }

    private fun withOpenedDatabase(database: AppDatabase, block: (AppDatabase) -> Unit) {
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun withDatabaseFile(block: (String) -> Unit) {
        val name = "database-initialization-${UUID.randomUUID()}.db"
        try {
            block(name)
        } finally {
            context.deleteDatabase(name)
        }
    }
}
