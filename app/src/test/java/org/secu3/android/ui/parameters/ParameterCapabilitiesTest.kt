package org.secu3.android.ui.parameters

import org.junit.Assert.*
import org.junit.Test
import org.secu3.android.R
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.input.FnNameDatPacket
import org.secu3.android.models.packets.out.params.*
import org.secu3.android.ui.parameters.pages.compose.*

class ParameterCapabilitiesTest {
    @Test fun flagsDistinguishHallAndWheelIgnitionAndOptionalHardware() {
        val wheel = parameterCapabilities(FirmwareInfoPacket(options = 0))
        assertTrue(wheel.ckps); assertTrue(wheel.ignitionCogs)
        assertFalse(wheel.hallWindow); assertFalse(wheel.fanPwm); assertFalse(wheel.choke)
        assertFalse(wheel.fuelInjection); assertFalse(wheel.oddCylinders)
        val hall = parameterCapabilities(FirmwareInfoPacket(options = (1 shl 24) or (1 shl 7) or (1 shl 13)))
        assertFalse(hall.ckps); assertFalse(hall.ignitionCogs); assertFalse(hall.camRef)
        assertTrue(hall.hallWindow); assertTrue(hall.risingSpark); assertTrue(hall.oddCylinders)
        assertFalse(hall.mttf)
        val injection = parameterCapabilities(FirmwareInfoPacket(options = (1 shl 28) or (1 shl 8) or (1 shl 16) or (1 shl 22)))
        assertTrue(injection.fuelInjection); assertTrue(injection.fanPwm)
        assertTrue(injection.tempCurve); assertTrue(injection.choke); assertFalse(injection.carburetor)
    }

    @Test fun chokePageIsOnlyAvailableForExplicitlySupportedFirmware() {
        assertFalse(parameterPageSpecs(FirmwareInfoPacket()).any { it.titleRes == R.string.params_tab_choke_control_title })
        assertEquals(R.string.params_tab_choke_control_title, parameterPageSpecs(FirmwareInfoPacket(options = 1 shl 22))[10].titleRes)
    }

    @Test fun missingToothAndTableModesGateOnlyTheirDependentFields() {
        val c = parameterCapabilities(FirmwareInfoPacket(options = 1 shl 28))
        val ckps = CkpsValues.from(CkpsParamPacket(ckpsMissNum = 0))
        assertFalse(ckpsNumberEnabled(CkpsNumber.MTTF, ckps, c))
        assertTrue(ckpsNumberEnabled(CkpsNumber.MTTF, ckps.withNumber(CkpsNumber.MISSING, 2f), c))
        val idle = IdlingValues.from(IdlingParamPacket())
        assertTrue(idlingNumberEnabled(IdlingNumber.IAC_ADD, idle, c))
        assertFalse(idlingNumberEnabled(IdlingNumber.IAC_ADD, idle.withFlag(IdlingFlag.THRASSMAP, true), c))
        assertTrue(idlingNumberEnabled(IdlingNumber.IFAC1, idle, c.copy(fuelInjection = false)))
    }

    @Test fun wheelGeometryAndIgnitionModeDetermineCkpsChoices() {
        assertEquals((11..55).toList(), ckpsTdcOptions(60, 4))
        assertEquals((2..10).toList(), ckpsTdcOptions(10, 4))
        assertEquals((11..90).toList(), ckpsTdcOptions(60, 2))
        assertTrue(ckpsTdcOptions(0, 4).isEmpty())
        val v = CkpsValues.from(CkpsParamPacket())
        val c = parameterCapabilities(FirmwareInfoPacket())
        assertEquals(listOf(2, 4, 6, 8), ckpsCylinderOptions(v, c))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 8), ckpsCylinderOptions(v.copy(mergeOuts = true), c))
        assertEquals((1..8).toList(), ckpsCylinderOptions(v.copy(mergeOuts = true), c.copy(sevenCylinders = true)))
    }

    @Test fun ignitionOnlyMafCanChangeDisplacementAndOnlyItsSharedFields() {
        val sent = mutableListOf<FunSetParamPacket>()
        val h = FunctionsStateHolder(sent::add)
        h.onNamesReceived(FnNameDatPacket())
        val p = FunSetParamPacket(ckpsEngineCyl = 4, injCylDisp = .5f, tpsCurveGradient = -2.5f)
        h.onPacketReceived(p, listOf("MAP", "MAP baro", "TPS", "MAP+TPS", "MAF"), emptyList(), emptyList())
        h.updateChoice(FunctionsChoices.Selector.LOAD_SOURCE, 4)
        assertEquals(4, (h.state as FunctionsUiState.Data).values.loadSource)
        assertEquals(2f, h.engineDisplacement!!, 0f)
        h.updateEngineDisplacement(3f)
        assertEquals(.75f, p.injCylDisp, 0f)
        assertTrue(p.mafload_const > 0)
        assertEquals(-2.5f, p.tpsCurveGradient, 0f)
        assertEquals(2, sent.size)
        h.updateEngineDisplacement(0f)
        assertEquals(2, sent.size)
    }
}
