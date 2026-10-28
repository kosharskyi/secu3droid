package org.secu3.android.domain.parameters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.input.FirmwareInfoPacket

class ParameterCapabilitiesTest {
    @Test fun firmwareOptionsDistinguishHallCkpsAndOptionalHardware() {
        val wheel = ParameterCapabilities(FirmwareInfoPacket(options = 0))
        assertTrue(wheel.ckps)
        assertTrue(wheel.ignitionCogs)
        assertFalse(wheel.hallWindow)
        assertFalse(wheel.fanPwm)
        assertFalse(wheel.choke)
        assertFalse(wheel.fuelInjection)
        assertFalse(wheel.oddCylinders)

        val hall = ParameterCapabilities(FirmwareInfoPacket(options = (1 shl 24) or (1 shl 7) or (1 shl 13)))
        assertFalse(hall.ckps)
        assertFalse(hall.ignitionCogs)
        assertFalse(hall.camRef)
        assertTrue(hall.hallWindow)
        assertTrue(hall.risingSpark)
        assertTrue(hall.oddCylinders)
        assertFalse(hall.mttf)

        val injection = ParameterCapabilities(FirmwareInfoPacket(options = (1 shl 28) or (1 shl 8) or (1 shl 16) or (1 shl 22)))
        assertTrue(injection.fuelInjection)
        assertTrue(injection.fanPwm)
        assertTrue(injection.tempCurve)
        assertTrue(injection.choke)
        assertFalse(injection.carburetor)

        val nPlusOne = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 31))
        assertFalse(nPlusOne.ckps)
        assertTrue(nPlusOne.hallWindow)
        assertFalse(nPlusOne.ignitionCogs)
    }

    @Test fun optionalPagesPreserveIndependentInjectionGasCarbAndBoardRules() {
        val injection = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 28))
        assertTrue(injection.supports(ParameterPage.FUEL_INJECTION))
        assertTrue(injection.supports(ParameterPage.LAMBDA_CONTROL))
        assertTrue(injection.supports(ParameterPage.ACCELERATION))
        assertTrue(injection.supports(ParameterPage.LTFT))
        assertFalse(injection.supports(ParameterPage.GAS_DOSE))
        assertTrue(injection.supports(ParameterPage.EL_THROTTLE))
        assertTrue(injection.lambdaChannelSelection)
        assertTrue(injection.lambdaStoich)

        val gasDose = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 29))
        assertFalse(gasDose.supports(ParameterPage.FUEL_INJECTION))
        assertTrue(gasDose.supports(ParameterPage.LAMBDA_CONTROL))
        assertTrue(gasDose.supports(ParameterPage.ACCELERATION))
        assertTrue(gasDose.supports(ParameterPage.GAS_DOSE))
        assertFalse(gasDose.supports(ParameterPage.LTFT))
        assertTrue(gasDose.supports(ParameterPage.EL_THROTTLE))
        assertTrue(gasDose.lambdaChannelSelection)
        assertTrue(gasDose.lambdaStoich)

        val carbAfr = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 30))
        assertTrue(carbAfr.supports(ParameterPage.LAMBDA_CONTROL))
        assertTrue(carbAfr.lambdaChannelSelection)
        assertFalse(carbAfr.lambdaStoich)
        assertTrue(carbAfr.supports(ParameterPage.EL_THROTTLE))

        val noLambdaSupport = ParameterCapabilities(FirmwareInfoPacket(options = 0))
        assertFalse(noLambdaSupport.supports(ParameterPage.LAMBDA_CONTROL))
        assertFalse(noLambdaSupport.lambdaChannelSelection)
        assertFalse(noLambdaSupport.lambdaStoich)

        val secu3t = ParameterCapabilities(FirmwareInfoPacket(options = (1 shl 28) or (1 shl 17)))
        assertFalse(secu3t.nonSecu3TFuelInjection)
        assertFalse(secu3t.supports(ParameterPage.EL_THROTTLE))
        assertTrue(ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 28)).nonSecu3TFuelInjection)
    }

    @Test fun unknownFirmwarePermitsFieldPreviewsButKeepsOptionalPagesAndLambdaChannelHidden() {
        val unknown = ParameterCapabilities(null)
        assertFalse(unknown.known)
        assertTrue(unknown.fuelInjection)
        assertTrue(unknown.gasDose)
        assertTrue(unknown.choke)
        assertTrue(unknown.ckps)
        assertTrue(unknown.hallWindow)
        assertTrue(unknown.dbw)
        assertTrue(unknown.oddCylinders)
        assertTrue(unknown.sevenCylinders)
        assertTrue(unknown.supports(ParameterPage.CHOKE_CONTROL))
        assertFalse(unknown.supports(ParameterPage.FUEL_INJECTION))
        assertFalse(unknown.supports(ParameterPage.LAMBDA_CONTROL))
        assertFalse(unknown.supports(ParameterPage.GAS_DOSE))
        assertFalse(unknown.lambdaChannelSelection)
        assertFalse(unknown.lambdaStoich)
        assertTrue(unknown.mttfEnabled(0f))
        assertTrue(unknown.camReferenceEnabled(2f))
        assertEquals((1..8).toList(), unknown.ckpsCylinderOptions(mergeOutputs = false))
    }

    @Test fun dependentFieldsUseCurrentModeWithFirmwareCapability() {
        val injection = ParameterCapabilities(FirmwareInfoPacket(options = (1 shl 28) or (1 shl 13)))
        assertTrue(injection.iacAdditionEnabled(throttleAsMap = false))
        assertFalse(injection.iacAdditionEnabled(throttleAsMap = true))
        assertFalse(injection.mttfEnabled(missingTeeth = 0f))
        assertTrue(injection.mttfEnabled(missingTeeth = 2f))
        assertTrue(injection.camReferenceEnabled(missingTeeth = 0f))
        assertFalse(injection.camReferenceEnabled(missingTeeth = 2f))

        val noOddMode = ParameterCapabilities(FirmwareInfoPacket(options = 0))
        assertEquals(listOf(2, 4, 6, 8), noOddMode.ckpsCylinderOptions(mergeOutputs = false))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 8), noOddMode.ckpsCylinderOptions(mergeOutputs = true))
        val seven = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 2))
        assertEquals((1..8).toList(), seven.ckpsCylinderOptions(mergeOutputs = true))
    }

    @Test fun accelerationTimeRequiresInjectionAndInjectionAeMode() {
        val injection = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 28))
        assertTrue(injection.accelerationTimeEnabled(aeType = 1))
        assertFalse(injection.accelerationTimeEnabled(aeType = 2))

        val gasDoseOnly = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 29))
        assertFalse(gasDoseOnly.accelerationTimeEnabled(aeType = 1))

        val unknown = ParameterCapabilities(null)
        assertTrue(unknown.accelerationTimeEnabled(aeType = 1))
        assertFalse(unknown.accelerationTimeEnabled(aeType = 2))
    }

    @Test fun snapshotDoesNotFollowLaterFirmwarePacketMutation() {
        val firmware = FirmwareInfoPacket(options = 1 shl 28)
        val snapshot = ParameterCapabilities(firmware)

        firmware.options = 0

        assertTrue(snapshot.fuelInjection)
        assertTrue(snapshot.supports(ParameterPage.FUEL_INJECTION))
        assertFalse(snapshot.supports(ParameterPage.GAS_DOSE))
    }
}
