package org.secu3.android.domain.parameters

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.secu3.android.models.packets.base.OutputPacket
import org.secu3.android.models.packets.input.FirmwareInfoPacket
import org.secu3.android.models.packets.out.params.LambdaParamPacket

class LambdaControlTest {
    private fun control(firmware: MutableStateFlow<FirmwareInfoPacket?> = MutableStateFlow(FirmwareInfoPacket(options = 1 shl 28)), admit: (OutputPacket) -> Boolean = { true }) =
        LambdaControl(firmware, admit)

    @Test fun receiveAndResetArePassiveAndPublishEveryNamedField() {
        var sends = 0
        val subject = control(admit = { sends++; true })
        val packet = LambdaParamPacket(strPerStp = 3, msPerStp = 120, stepSizeP = 1.25f, stepSizeM = 2.5f,
            corrLimitP = 25f, corrLimitM = 30f, swtPoint = .5f, tempThrd = -12.5f, rpmThrd = 1200,
            activDelay = 42, deadBand = .125f, senstype = 7, flags = 0b1101, gdStoichval = 14.7f,
            heatingTime0 = 12, heatingTime1 = 34, temperThrd = 75, heatingAct = .25f,
            aflowThrd = 320000f, lambdaSelectedChanel = 0x81)
        assertEquals(null, subject.values.value)
        subject.receive(packet)
        assertEquals(LambdaValues(
            strokesPerStep = 3, msPerStep = 120, positiveStep = 1.25f, negativeStep = 2.5f,
            positiveLimit = 25f, negativeLimit = 30f, switchPoint = .5f, coolantThreshold = -12.5f,
            rpmThreshold = 1200, activationDelay = 42, deadband = .125f, sensorTypeCode = 7,
            stoichiometricRatio = 14.7f, coldHeatingTime = 12, hotHeatingTime = 34, heatingTemperature = 75,
            heatingPwmTime = .25f, airFlowThreshold = 320000f, heatingVoltage = true,
            idlingCorrection = false, beforeCranking = true, mixSensors = true,
            selectedChannels = listOf(true, false, false, false, false, false, false, true),
        ), subject.values.value)
        assertEquals(0, sends)
        subject.reset()
        assertEquals(null, subject.values.value)
        assertFalse(subject.editStrokesPerStep(1))
        assertEquals(0, sends)
    }

    @Test fun allSeventeenNamedEditsRetainTheirOtherDistinctFields() {
        val sent = mutableListOf<IntArray>()
        val subject = control(admit = { sent += it.pack(); true })
        val packet = LambdaParamPacket(strPerStp = 0, msPerStp = 2550, stepSizeP = 1.25f, stepSizeM = 2.5f,
            corrLimitP = 25f, corrLimitM = 50f, swtPoint = .5f, tempThrd = -12.5f, rpmThrd = 1500,
            activDelay = 42, deadBand = .125f, senstype = 7, flags = 0xA0, gdStoichval = 16f,
            heatingTime0 = 12, heatingTime1 = 34, temperThrd = 75, heatingAct = .25f,
            aflowThrd = 320000f, lambdaSelectedChanel = 0xA5)
        subject.receive(packet)
        val initial = requireNotNull(subject.values.value)
        val edits = listOf(
            initial.copy(msPerStep = 120) to { subject.editMsPerStep(129) },
            initial.copy(msPerStep = 120, strokesPerStep = 9) to { subject.editStrokesPerStep(9) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f) to { subject.editPositiveStep(3.25f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f) to { subject.editNegativeStep(4.5f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f) to { subject.editPositiveLimit(26f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f) to { subject.editNegativeLimit(51f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f) to { subject.editSwitchPoint(.75f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f) to { subject.editCoolantThreshold(-18.25f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500) to { subject.editRpmThreshold(2500) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43) to { subject.editActivationDelay(43) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43, deadband = .25f) to { subject.editDeadband(.25f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43, deadband = .25f, stoichiometricRatio = 17.2f) to { subject.editStoichiometricRatio(17.2f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43, deadband = .25f, stoichiometricRatio = 17.2f, coldHeatingTime = 13) to { subject.editColdHeatingTime(13) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43, deadband = .25f, stoichiometricRatio = 17.2f, coldHeatingTime = 13, hotHeatingTime = 35) to { subject.editHotHeatingTime(35) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43, deadband = .25f, stoichiometricRatio = 17.2f, coldHeatingTime = 13, hotHeatingTime = 35, heatingTemperature = 76) to { subject.editHeatingTemperature(76) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43, deadband = .25f, stoichiometricRatio = 17.2f, coldHeatingTime = 13, hotHeatingTime = 35, heatingTemperature = 76, heatingPwmTime = .26f) to { subject.editHeatingPwmTime(.26f) },
            initial.copy(msPerStep = 120, strokesPerStep = 9, positiveStep = 3.25f, negativeStep = 4.5f, positiveLimit = 26f, negativeLimit = 51f, switchPoint = .75f, coolantThreshold = -18.25f, rpmThreshold = 2500, activationDelay = 43, deadband = .25f, stoichiometricRatio = 17.2f, coldHeatingTime = 13, hotHeatingTime = 35, heatingTemperature = 76, heatingPwmTime = .26f, airFlowThreshold = 330000f) to { subject.editAirFlowThreshold(330000) },
        )
        edits.forEach { (expected, command) ->
            assertTrue(command())
            assertEquals(expected, subject.values.value)
        }
        assertEquals(17, sent.size)
        assertEquals(9, subject.values.value?.strokesPerStep)
        assertEquals(120, subject.values.value?.msPerStep)
        assertEquals(3.25f, subject.values.value?.positiveStep)
        assertEquals(4.5f, subject.values.value?.negativeStep)
        assertEquals(26f, subject.values.value?.positiveLimit)
        assertEquals(51f, subject.values.value?.negativeLimit)
        assertEquals(.75f, subject.values.value?.switchPoint)
        assertEquals(-18.25f, subject.values.value?.coolantThreshold)
        assertEquals(2500, subject.values.value?.rpmThreshold)
        assertEquals(43, subject.values.value?.activationDelay)
        assertEquals(.25f, subject.values.value?.deadband)
        assertEquals(17.2f, subject.values.value?.stoichiometricRatio)
        assertEquals(13, subject.values.value?.coldHeatingTime)
        assertEquals(35, subject.values.value?.hotHeatingTime)
        assertEquals(76, subject.values.value?.heatingTemperature)
        assertEquals(.26f, subject.values.value?.heatingPwmTime)
        assertEquals(330000f, subject.values.value?.airFlowThreshold)
    }

    @Test fun goldenWireVectorAndImmutableSnapshotsPreserveTailRawFlagsAndUnknownCodes() {
        val sends = mutableListOf<OutputPacket>()
        val subject = control(admit = { sends += it; true })
        val packet = LambdaParamPacket(strPerStp = 0, stepSizeP = 6.25f, stepSizeM = 12.5f,
            corrLimitP = 25f, corrLimitM = 50f, swtPoint = .5f, tempThrd = -12.5f, rpmThrd = 1500,
            activDelay = 42, deadBand = .125f, senstype = 1, msPerStp = 2550, flags = 0xA0,
            gdStoichval = 16f, heatingTime0 = 12, heatingTime1 = 34, temperThrd = 75,
            heatingAct = .25f, aflowThrd = 320000f, lambdaSelectedChanel = 0xA5)
        // Parse a complete read so the packet owns an unhandled tail beyond its 28-byte payload.
        val parsed = LambdaParamPacket().apply { parse(intArrayOf(0, 0) + packet.pack().copyOfRange(1, 29) + intArrayOf(0x71, 0x72)) }
        subject.receive(parsed)
        assertEquals(1, subject.values.value?.sensorTypeCode)
        assertTrue(subject.editPositiveStep(7.5f))
        assertEquals(2550, parsed.msPerStp)
        assertEquals(0xA0, parsed.flags)
        assertEquals(0xA5, parsed.lambdaSelectedChanel)
        assertEquals(0x71, sends.single().pack()[29])
        assertEquals(0x72, sends.single().pack()[30])
        assertEquals(0xA0, sends.single().pack()[19])
        assertEquals(0xA5, sends.single().pack()[28])
        assertTrue(subject.selectSensorType(0))
        assertEquals(1, sends[0].pack()[17]) // first snapshot is frozen
        assertEquals(0, sends[1].pack()[17])
    }

    @Test fun parsedUnknownSensorSignedFractionAndTailSurviveRefusedEditExactly() {
        val subject = control(admit = { false })
        val source = LambdaParamPacket(strPerStp = 0, stepSizeP = 1.25f, stepSizeM = 2.5f,
            corrLimitP = 25f, corrLimitM = 50f, swtPoint = .5f, tempThrd = -12.5f, rpmThrd = 1500,
            activDelay = 42, deadBand = .125f, senstype = 7, msPerStp = 2550, flags = 0xA0,
            gdStoichval = 16f, heatingTime0 = 12, heatingTime1 = 34, temperThrd = 75,
            heatingAct = .25f, aflowThrd = 320000f, lambdaSelectedChanel = 0xA5)
        val parsed = LambdaParamPacket().apply {
            parse(intArrayOf(0, 0) + source.pack().copyOfRange(1, 29) + intArrayOf(0x71, 0x72))
        }
        subject.receive(parsed)
        val before = parsed.pack().copyOf()
        assertEquals(7, subject.values.value?.sensorTypeCode)
        assertEquals(-12.5f, subject.values.value?.coolantThreshold)
        assertEquals(2550, subject.values.value?.msPerStep)
        assertFalse(subject.editPositiveStep(3f))
        assertArrayEquals(before, parsed.pack())
    }


    @Test fun approvedGoldenPayloadUsesTheExistingTwentyEightByteWireLayout() {
        val sent = mutableListOf<IntArray>()
        val subject = control(admit = { sent += it.pack(); true })
        subject.receive(LambdaParamPacket(strPerStp = 0, stepSizeP = 6.25f, stepSizeM = 12.5f,
            corrLimitP = 25f, corrLimitM = 50f, swtPoint = .5f, tempThrd = -12.5f, rpmThrd = 1500,
            activDelay = 42, deadBand = .125f, senstype = 1, msPerStp = 129, flags = 0xA0,
            gdStoichval = 16f, heatingTime0 = 12, heatingTime1 = 34, temperThrd = 75,
            heatingAct = .25f, aflowThrd = 320000f, lambdaSelectedChanel = 0xA5))
        assertTrue(subject.editMsPerStep(129))
        assertEquals(29, sent.single().size)
        assertArrayEquals(intArrayOf('-'.code, 0,32,64,0,128,1,0,0,200,255,206,5,220,42,0,50,1,12,0xA0,8,0,12,34,75,25,39,16,0xA5), sent.single())
    }

    @Test fun invalidRangesAndNonFiniteFloatsAreRejectedBeforeAnAcceptingSenderIsCalled() {
        var sends = 0
        val subject = control(admit = { sends++; true })
        subject.receive(LambdaParamPacket(strPerStp = 0))
        val invalidCommands = listOf<() -> Boolean>(
            { subject.editStrokesPerStep(-1) }, { subject.editStrokesPerStep(256) },
            { subject.editMsPerStep(-10) }, { subject.editMsPerStep(2501) },
            { subject.editPositiveStep(-.01f) }, { subject.editPositiveStep(45.01f) },
            { subject.editNegativeStep(Float.NaN) }, { subject.editPositiveLimit(Float.POSITIVE_INFINITY) },
            { subject.editNegativeLimit(.49f) }, { subject.editSwitchPoint(5.01f) },
            { subject.editCoolantThreshold(Float.NEGATIVE_INFINITY) }, { subject.editCoolantThreshold(-40.01f) },
            { subject.editRpmThreshold(30001) }, { subject.editActivationDelay(-1) },
            { subject.editDeadband(-.01f) }, { subject.editDeadband(Float.NaN) },
            { subject.editStoichiometricRatio(7.99f) }, { subject.editStoichiometricRatio(Float.POSITIVE_INFINITY) },
            { subject.editColdHeatingTime(256) }, { subject.editHotHeatingTime(-1) },
            { subject.editHeatingTemperature(256) }, { subject.editHeatingPwmTime(2.56f) },
            { subject.editHeatingPwmTime(Float.NaN) }, { subject.editAirFlowThreshold(99) },
            { subject.editAirFlowThreshold(2_000_001) },
        )
        invalidCommands.forEach { assertFalse(it()) }
        assertEquals(0, sends)
    }

    @Test fun flagsChannelsSensorCodesAndInvalidInputsPreserveUnknownBits() {
        val sent = mutableListOf<OutputPacket>()
        val subject = control(admit = { sent += it; true })
        val packet = LambdaParamPacket(flags = 0xF0, lambdaSelectedChanel = 0x00, senstype = 255)
        subject.receive(packet)
        assertEquals(255, subject.values.value?.sensorTypeCode)
        assertTrue(subject.setHeatingVoltage(true)); assertTrue(subject.setIdlingCorrection(true))
        assertTrue(subject.setBeforeCranking(true)); assertTrue(subject.setMixSensors(true))
        assertEquals(0xFF, packet.flags)
        for (i in 0..7) assertTrue(subject.setChannelSelected(i, true))
        assertEquals(0xFF, packet.lambdaSelectedChanel)
        assertFalse(subject.setChannelSelected(-1, false)); assertFalse(subject.setChannelSelected(8, false))
        assertFalse(subject.selectSensorType(2)); assertFalse(subject.selectSensorType(-1))
        assertEquals(12, sent.size)
        assertEquals(8, subject.values.value?.selectedChannels?.count { it })
        assertEquals(0xFF, packet.flags)
    }

    @Test fun invalidBoundsWrongFloatValuesAndRefusedSendsDoNotMutateState() {
        var admissions = 0
        val subject = control(admit = { admissions++; false })
        val packet = LambdaParamPacket(strPerStp = 0, stepSizeP = 2f)
        subject.receive(packet)
        assertFalse(subject.editPositiveStep(Float.NaN)); assertFalse(subject.editPositiveStep(Float.POSITIVE_INFINITY))
        assertFalse(subject.editPositiveStep(-.1f)); assertFalse(subject.editRpmThreshold(-1))
        assertEquals(0, admissions)
        assertFalse(subject.editPositiveStep(3f))
        assertEquals(2f, packet.stepSizeP); assertEquals(2f, subject.values.value?.positiveStep)
        assertEquals(1, admissions)
    }


    @Test fun outOfRangeReceivedMsSurvivesUnrelatedStrokesEdit() {
        val subject = control(admit = { true })
        val packet = LambdaParamPacket(msPerStp = 2550)
        subject.receive(packet)
        assertTrue(subject.editStrokesPerStep(1))
        assertEquals(2550, packet.msPerStp)
        assertFalse(subject.editMsPerStep(2501))
        assertTrue(subject.editStrokesPerStep(0))
        assertTrue(subject.editMsPerStep(129))
        assertEquals(120, packet.msPerStp)
    }

    @Test fun capabilitiesAndLiveStaleCommandsFollowApprovedLambdaRules() {
        val fw = MutableStateFlow<FirmwareInfoPacket?>(FirmwareInfoPacket(options = 1 shl 30))
        val subject = control(fw)
        val packet = LambdaParamPacket(strPerStp = 0)
        subject.receive(packet)
        assertTrue(subject.editStrokesPerStep(1))
        assertFalse(subject.editMsPerStep(130))
        assertTrue(subject.setChannelSelected(0, true)) // supported CARB_AFR
        assertFalse(subject.editStoichiometricRatio(15f))
        assertTrue(subject.editStrokesPerStep(0))
        assertTrue(subject.editMsPerStep(130))
        fw.value = FirmwareInfoPacket(options = 0)
        assertFalse(subject.editPositiveStep(3f)); assertFalse(subject.editMsPerStep(120))
        assertFalse(subject.setChannelSelected(0, false))
        fw.value = null
        assertTrue(subject.editPositiveStep(3f)); assertFalse(subject.setChannelSelected(0, true))
        assertFalse(subject.editStoichiometricRatio(15f))
    }

    @Test fun lambdaCapabilitiesKeepUnknownBasePermissiveButOptionalRulesStrict() {
        val injection = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 28))
        assertTrue(injection.lambdaBaseEditing); assertTrue(injection.lambdaChannelSelection); assertTrue(injection.lambdaStoich)
        val gasDose = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 29))
        assertTrue(gasDose.lambdaBaseEditing); assertTrue(gasDose.lambdaChannelSelection); assertTrue(gasDose.lambdaStoich)
        val carbAfr = ParameterCapabilities(FirmwareInfoPacket(options = 1 shl 30))
        assertTrue(carbAfr.lambdaBaseEditing); assertTrue(carbAfr.lambdaChannelSelection); assertFalse(carbAfr.lambdaStoich)
        val unsupported = ParameterCapabilities(FirmwareInfoPacket(options = 0))
        assertFalse(unsupported.lambdaBaseEditing); assertFalse(unsupported.lambdaChannelSelection); assertFalse(unsupported.lambdaStoich)
        val unknown = ParameterCapabilities(null)
        assertTrue(unknown.lambdaBaseEditing); assertFalse(unknown.lambdaChannelSelection); assertFalse(unknown.lambdaStoich)
        assertTrue(unknown.lambdaMsEditing(0)); assertFalse(unknown.lambdaMsEditing(1))
    }
}
